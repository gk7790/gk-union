package com.gk.telegram.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.gk.platform.entity.SysUserSubjectEntity;
import com.gk.platform.service.SysUserSubjectService;
import com.gk.telegram.command.TgCommandContext;
import com.gk.telegram.command.TgCommandDispatcher;
import com.gk.telegram.entity.TgAccountEntity;
import com.gk.telegram.entity.TgBotEntity;
import com.gk.telegram.entity.TgChatEntity;
import com.gk.telegram.entity.TgUpdateLogEntity;
import com.gk.telegram.service.TgAccountService;
import com.gk.telegram.service.TgBotService;
import com.gk.telegram.service.TgChatService;
import com.gk.telegram.service.TgUpdateLogService;
import com.gk.telegram.service.TgWebhookService;
import com.gk.telegram.support.TgConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Telegram Webhook 入站处理服务实现。
 * <p>负责机器人鉴权、update 幂等登记、命令上下文构建、指令分发和 webhook 直回消息封装。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TgWebhookServiceImpl implements TgWebhookService {
    /** 机器人启用状态 */
    private static final int BOT_ENABLED = 1;
    /** webhook 直返消息的渲染模式(各指令回复均输出 HTML, 动态值经 TgHtml 转义) */

    private final TgBotService tgBotService;
    private final TgAccountService tgAccountService;
    private final TgChatService tgChatService;
    private final TgUpdateLogService tgUpdateLogService;
    private final TgCommandDispatcher commandDispatcher;
    private final SysUserSubjectService sysUserSubjectService;

    /**
     * 处理 Telegram webhook 推送的一条 Update。
     * <p>
     * 入口职责包括机器人鉴权、Update 幂等登记、解析命令上下文、分发指令并构造 Telegram webhook 直返响应。
     */
    @Override
    public Result handle(String botNo, String secretToken, String rawBody) {
        TgBotEntity bot = tgBotService.getByBotNo(botNo);
        // 机器人不存在或停用: 视为未授权(不暴露细节)
        if (bot == null || bot.getStatus() == null || bot.getStatus() != BOT_ENABLED) {
            return Result.unauthorized();
        }
        // 校验 webhook secret_token (设置了才校验)
        if (StringUtils.isNotBlank(bot.getSecretToken())
                && !bot.getSecretToken().equals(secretToken)) {
            return Result.unauthorized();
        }

        if (StringUtils.isBlank(rawBody)) {
            return Result.ok(null);
        }

        JSONObject root;
        try {
            root = JSON.parseObject(rawBody);
        } catch (Exception e) {
            log.warn("Telegram webhook parse error, botNo={}, err={}", botNo, e.getMessage());
            return Result.ok(null);
        }
        if (root == null || root.get("update_id") == null) {
            return Result.ok(null);
        }
        Long updateId = root.getLong("update_id");

        // 仅处理 message 文本(其余类型先记录, 不回复)
        JSONObject message = root.getJSONObject("message");
        JSONObject from = message != null ? message.getJSONObject("from") : null;
        JSONObject chat = message != null ? message.getJSONObject("chat") : null;
        String text = message != null ? message.getString("text") : null;

        Long tgUserId = from != null ? from.getLong("id") : null;
        Long chatId = chat != null ? chat.getLong("id") : null;
        String chatType = chat != null ? chat.getString("type") : null;
        String chatTitle = chat != null ? chat.getString("title") : null;
        String tgUsername = from != null ? from.getString("username") : null;
        String languageCode = from != null ? from.getString("language_code") : null;
        String command = parseCommand(text);

        // 幂等登记(bot_id + update_id 唯一)
        TgUpdateLogEntity logEntity = new TgUpdateLogEntity();
        logEntity.setBotId(bot.getId());
        logEntity.setUpdateId(updateId);
        logEntity.setTgUserId(tgUserId);
        logEntity.setChatId(chatId);
        logEntity.setUpdateType(message != null ? "message" : resolveType(root));
        logEntity.setCommand(command);
        logEntity.setRawJson(rawBody);
        boolean first = tgUpdateLogService.tryBegin(logEntity);
        if (!first) {
            // 重复投递, 幂等跳过
            return Result.ok(null);
        }

        // 非文本/非命令消息: 不处理, 标记成功
        if (StringUtils.isBlank(text) || command == null || chatId == null) {
            tgUpdateLogService.markResult(logEntity.getId(), 1, null);
            return Result.ok(null);
        }

        String replyText;
        try {
            // 私聊只走个人绑定，群/超级群只走群绑定，避免群指令被发送人的个人账号范围覆盖。
            boolean privateChat = "private".equalsIgnoreCase(chatType);
            boolean groupChat = "group".equalsIgnoreCase(chatType) || "supergroup".equalsIgnoreCase(chatType);
            TgAccountEntity account = privateChat && tgUserId != null
                    ? tgAccountService.getActiveBinding(bot.getId(), tgUserId)
                    : null;
            TgChatEntity boundChat = groupChat && chatId != null
                    ? tgChatService.getActiveChat(bot.getId(), chatId)
                    : null;
            SysUserSubjectEntity subject = account != null && account.getSubjectId() != null
                    ? sysUserSubjectService.selectById(account.getSubjectId())
                    : null;
            Long targetTenantId = subject != null ? subject.getTenantId() : (boundChat == null ? null : boundChat.getTenantId());
            Long targetMerchantId = subject != null ? subject.getMerchantId() : (boundChat == null ? null : boundChat.getMerchantId());
            TgCommandContext ctx = TgCommandContext.builder()
                    .bot(bot)
                    .tgUserId(tgUserId)
                    .tgUsername(tgUsername)
                    .languageCode(languageCode)
                    .chatId(chatId)
                    .chatType(chatType)
                    .chatTitle(chatTitle)
                    .rawText(text)
                    .command(command)
                    .args(parseArgs(text))
                    .account(account)
                    .chat(boundChat)
                    .targetTenantId(targetTenantId)
                    .targetMerchantId(targetMerchantId)
                    .build();
            // 指令自身只返回回复文本，webhook 层负责包装成 Telegram sendMessage 响应体。
            replyText = commandDispatcher.dispatch(ctx);
            tgUpdateLogService.markResult(logEntity.getId(), 1, null);
        } catch (Exception e) {
            log.error("Telegram command process error, botNo={}, updateId={}", botNo, updateId, e);
            tgUpdateLogService.markResult(logEntity.getId(), 2, StringUtils.abbreviate(e.getMessage(), 1000));
            replyText = "处理指令时发生错误, 请稍后再试。";
        }

        return Result.ok(buildSendMessage(chatId, replyText));
    }

    /**
     * Telegram webhook 直返方式: 响应体即为一个待执行的 Bot API 方法
     */
    private Map<String, Object> buildSendMessage(Long chatId, String text) {
        if (StringUtils.isBlank(text)) {
            return null;
        }
        Map<String, Object> reply = new LinkedHashMap<>();
        // Telegram 支持 webhook 响应体直接声明要执行的 Bot API 方法，这里用来省一次 sendMessage HTTP 调用。
        reply.put("method", "sendMessage");
        reply.put("chat_id", chatId);
        reply.put("text", text);
        reply.put("parse_mode", TgConstants.ParseMode.HTML);
        return reply;
    }

    /**
     * 解析命令(首token, 去除 @botusername 后缀, 转小写); 非命令返回null
     */
    private String parseCommand(String text) {
        if (StringUtils.isBlank(text)) {
            return null;
        }
        String trimmed = text.trim();
        if (!trimmed.startsWith("/")) {
            return null;
        }
        String first = trimmed.split("\\s+")[0];
        // 群里用户可能发送 /order@bot_username，这里去掉 bot 后缀后再匹配本地指令。
        int at = first.indexOf('@');
        if (at > 0) {
            first = first.substring(0, at);
        }
        return first.toLowerCase();
    }

    /**
     * 解析参数(命令之后的token)
     */
    private List<String> parseArgs(String text) {
        if (StringUtils.isBlank(text)) {
            return new ArrayList<>();
        }
        String[] parts = text.trim().split("\\s+");
        if (parts.length <= 1) {
            return new ArrayList<>();
        }
        return new ArrayList<>(Arrays.asList(parts).subList(1, parts.length));
    }

    /**
     * 识别当前暂未处理的 Telegram Update 类型，主要用于 tg_update_log 审计排查。
     */
    private String resolveType(JSONObject root) {
        if (root.containsKey("callback_query")) {
            return "callback_query";
        }
        if (root.containsKey("edited_message")) {
            return "edited_message";
        }
        if (root.containsKey("channel_post")) {
            return "channel_post";
        }
        return "unknown";
    }
}
