package com.gk.telegram.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.gk.telegram.command.TgCommandContext;
import com.gk.telegram.command.TgCommandDispatcher;
import com.gk.telegram.entity.TgAccountEntity;
import com.gk.telegram.entity.TgBotEntity;
import com.gk.telegram.entity.TgUpdateLogEntity;
import com.gk.telegram.service.TgAccountService;
import com.gk.telegram.service.TgBotService;
import com.gk.telegram.service.TgUpdateLogService;
import com.gk.telegram.service.TgWebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TgWebhookServiceImpl implements TgWebhookService {
    /** 机器人启用状态 */
    private static final int BOT_ENABLED = 1;

    private final TgBotService tgBotService;
    private final TgAccountService tgAccountService;
    private final TgUpdateLogService tgUpdateLogService;
    private final TgCommandDispatcher commandDispatcher;

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
            TgAccountEntity account = tgUserId != null
                    ? tgAccountService.getActiveBinding(bot.getId(), tgUserId)
                    : null;
            TgCommandContext ctx = TgCommandContext.builder()
                    .bot(bot)
                    .tgUserId(tgUserId)
                    .tgUsername(tgUsername)
                    .languageCode(languageCode)
                    .chatId(chatId)
                    .rawText(text)
                    .command(command)
                    .args(parseArgs(text))
                    .account(account)
                    .build();
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
        reply.put("method", "sendMessage");
        reply.put("chat_id", chatId);
        reply.put("text", text);
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
