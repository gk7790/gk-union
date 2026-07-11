package com.gk.telegram.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.gk.iam.entity.SysUserSubjectEntity;
import com.gk.iam.service.SysUserSubjectService;
import com.gk.telegram.callback.TgCallbackContext;
import com.gk.telegram.callback.TgCallbackDispatcher;
import com.gk.telegram.callback.TgCallbackResult;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class TgWebhookServiceImpl implements TgWebhookService {
    private static final int BOT_ENABLED = 1;

    private final TgBotService tgBotService;
    private final TgAccountService tgAccountService;
    private final TgChatService tgChatService;
    private final TgUpdateLogService tgUpdateLogService;
    private final TgCommandDispatcher commandDispatcher;
    private final TgCallbackDispatcher callbackDispatcher;
    private final SysUserSubjectService sysUserSubjectService;

    @Override
    public Result handle(String botNo, String secretToken, String rawBody) {
        TgBotEntity bot = tgBotService.getByBotNo(botNo);
        if (bot == null || bot.getStatus() == null || bot.getStatus() != BOT_ENABLED) {
            return Result.unauthorized();
        }
        if (StringUtils.isNotBlank(bot.getSecretToken()) && !bot.getSecretToken().equals(secretToken)) {
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

        JSONObject message = root.getJSONObject("message");
        JSONObject callbackQuery = root.getJSONObject("callback_query");
        if (callbackQuery != null) {
            return handleCallbackUpdate(bot, root, callbackQuery, rawBody);
        }
        if (message != null) {
            return handleMessageUpdate(bot, root, message, rawBody);
        }

        return recordUnsupportedUpdate(bot, root, rawBody);
    }

    private Result handleMessageUpdate(TgBotEntity bot, JSONObject root, JSONObject message, String rawBody) {
        Long updateId = root.getLong("update_id");
        JSONObject from = message.getJSONObject("from");
        JSONObject chat = message.getJSONObject("chat");
        String text = message.getString("text");

        Long tgUserId = from != null ? from.getLong("id") : null;
        Long chatId = chat != null ? chat.getLong("id") : null;
        String chatType = chat != null ? chat.getString("type") : null;
        String chatTitle = chat != null ? chat.getString("title") : null;
        String tgUsername = from != null ? from.getString("username") : null;
        String languageCode = from != null ? from.getString("language_code") : null;
        String command = parseCommand(text);

        TgUpdateLogEntity logEntity = buildLog(bot, updateId, tgUserId, chatId, "message", command, rawBody);
        if (!tgUpdateLogService.tryBegin(logEntity)) {
            return Result.ok(null);
        }

        if (StringUtils.isBlank(text) || command == null || chatId == null) {
            tgUpdateLogService.markResult(logEntity.getId(), 1, null);
            return Result.ok(null);
        }

        String replyText;
        try {
            TgCommandContext ctx = buildCommandContext(bot, tgUserId, tgUsername, languageCode,
                    chatId, chatType, chatTitle, text, command);
            replyText = commandDispatcher.dispatch(ctx);
            tgUpdateLogService.markResult(logEntity.getId(), 1, null);
        } catch (Exception e) {
            log.error("Telegram command process error, botId={}, updateId={}", bot.getId(), updateId, e);
            tgUpdateLogService.markResult(logEntity.getId(), 2, StringUtils.abbreviate(e.getMessage(), 1000));
            replyText = "处理指令时发生错误，请稍后再试。";
        }

        return Result.ok(buildSendMessage(chatId, replyText));
    }

    private Result handleCallbackUpdate(TgBotEntity bot, JSONObject root, JSONObject callbackQuery, String rawBody) {
        Long updateId = root.getLong("update_id");
        JSONObject from = callbackQuery.getJSONObject("from");
        JSONObject callbackMessage = callbackQuery.getJSONObject("message");
        JSONObject chat = callbackMessage != null ? callbackMessage.getJSONObject("chat") : null;
        String callbackData = callbackQuery.getString("data");

        Long tgUserId = from != null ? from.getLong("id") : null;
        Long chatId = chat != null ? chat.getLong("id") : null;
        String chatType = chat != null ? chat.getString("type") : null;
        String chatTitle = chat != null ? chat.getString("title") : null;
        String tgUsername = from != null ? from.getString("username") : null;
        String languageCode = from != null ? from.getString("language_code") : null;
        String callbackQueryId = callbackQuery.getString("id");
        Long messageId = callbackMessage != null ? callbackMessage.getLong("message_id") : null;
        String action = callbackAction(callbackData);

        TgUpdateLogEntity logEntity = buildLog(bot, updateId, tgUserId, chatId, "callback_query", action, rawBody);
        if (!tgUpdateLogService.tryBegin(logEntity)) {
            return Result.ok(null);
        }

        try {
            TgCallbackContext ctx = TgCallbackContext.builder()
                    .bot(bot)
                    .callbackQueryId(callbackQueryId)
                    .callbackData(callbackData)
                    .tgUserId(tgUserId)
                    .tgUsername(tgUsername)
                    .languageCode(languageCode)
                    .chatId(chatId)
                    .chatType(chatType)
                    .chatTitle(chatTitle)
                    .messageId(messageId)
                    .build();
            TgCallbackResult callbackResult = callbackDispatcher.dispatch(ctx);
            tgUpdateLogService.markResult(logEntity.getId(), 1, null);
            return Result.ok(buildAnswerCallbackQuery(callbackQueryId, callbackResult));
        } catch (Exception e) {
            log.error("Telegram callback process error, botId={}, updateId={}", bot.getId(), updateId, e);
            tgUpdateLogService.markResult(logEntity.getId(), 2, StringUtils.abbreviate(e.getMessage(), 1000));
            return Result.ok(buildAnswerCallbackQuery(callbackQueryId,
                    TgCallbackResult.toast("Action failed, please try again later")));
        }
    }

    private Result recordUnsupportedUpdate(TgBotEntity bot, JSONObject root, String rawBody) {
        TgUpdateLogEntity logEntity = buildLog(bot, root.getLong("update_id"), null, null,
                resolveType(root), null, rawBody);
        if (tgUpdateLogService.tryBegin(logEntity)) {
            tgUpdateLogService.markResult(logEntity.getId(), 1, null);
        }
        return Result.ok(null);
    }

    private TgCommandContext buildCommandContext(TgBotEntity bot,
                                                 Long tgUserId,
                                                 String tgUsername,
                                                 String languageCode,
                                                 Long chatId,
                                                 String chatType,
                                                 String chatTitle,
                                                 String text,
                                                 String command) {
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

        return TgCommandContext.builder()
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
    }

    private TgUpdateLogEntity buildLog(TgBotEntity bot,
                                       Long updateId,
                                       Long tgUserId,
                                       Long chatId,
                                       String updateType,
                                       String command,
                                       String rawBody) {
        TgUpdateLogEntity logEntity = new TgUpdateLogEntity();
        logEntity.setBotId(bot.getId());
        logEntity.setUpdateId(updateId);
        logEntity.setTgUserId(tgUserId);
        logEntity.setChatId(chatId);
        logEntity.setUpdateType(updateType);
        logEntity.setCommand(command);
        logEntity.setRawJson(rawBody);
        return logEntity;
    }

    private Map<String, Object> buildSendMessage(Long chatId, String text) {
        if (chatId == null || StringUtils.isBlank(text)) {
            return null;
        }
        Map<String, Object> reply = new LinkedHashMap<>();
        reply.put("method", "sendMessage");
        reply.put("chat_id", chatId);
        reply.put("text", text);
        reply.put("parse_mode", TgConstants.ParseMode.HTML);
        return reply;
    }

    private Map<String, Object> buildAnswerCallbackQuery(String callbackQueryId, TgCallbackResult result) {
        if (StringUtils.isBlank(callbackQueryId)) {
            return null;
        }
        Map<String, Object> reply = new LinkedHashMap<>();
        reply.put("method", "answerCallbackQuery");
        reply.put("callback_query_id", callbackQueryId);
        if (result != null && StringUtils.isNotBlank(result.getText())) {
            reply.put("text", result.getText());
            reply.put("show_alert", result.isShowAlert());
        }
        return reply;
    }

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

    private String callbackAction(String data) {
        if (StringUtils.isBlank(data)) {
            return null;
        }
        String[] parts = data.trim().split(":", 3);
        if (parts.length >= 2) {
            return (parts[0] + ":" + parts[1]).toLowerCase();
        }
        return parts[0].toLowerCase();
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
