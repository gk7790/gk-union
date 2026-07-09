package com.gk.telegram.bot;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.gk.common.constant.Constant;
import com.gk.common.model.Result;
import com.gk.infra.config.model.TgBaseConfig;
import com.gk.infra.config.service.SysParamsService;
import com.gk.telegram.support.TgConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Telegram Bot API 客户端，统一封装 getMe、setWebhook、sendMessage 等外部 API 调用。
 * <p>
 * 这里不直接抛出远端错误，而是转换成系统通用 {@link Result}，方便 controller/service 直接返回明确错误描述。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TgBotApiClient {
    private final SysParamsService sysParamsService;

    /**
     * 根据 sys_params 中的 Telegram 基础配置创建 RestClient。
     * <p>未配置时兜底使用官方 API 地址。</p>
     */
    private RestClient client() {
        TgBaseConfig tgBase = sysParamsService.getValueObject(Constant.TELEGRAM_BASE_CONFIG_KEY, TgBaseConfig.class);
        String apiBaseUrl = tgBase == null ? null : tgBase.getApiBaseUrl();
        if (apiBaseUrl == null || apiBaseUrl.isBlank()) {
            apiBaseUrl = "https://api.telegram.org";
        }
        return RestClient.builder().baseUrl(apiBaseUrl.trim()).build();
    }

    /**
     * 调用 Telegram getMe，用于校验 Bot Token 是否可用，并获取 bot username/userId。
     */
    public Result<JSONObject> getMe(String token) {
        try {
            String resp = client().get()
                    .uri("/bot" + token + "/getMe")
                    .retrieve()
                    .body(String.class);
            return result(resp, "Telegram getMe succeeded");
        } catch (Exception e) {
            log.warn("Telegram getMe failed: {}", e.getMessage());
            return fail(e);
        }
    }

    /**
     * 设置 Telegram webhook 地址。
     *
     * @param secretToken Telegram 会在回调请求头 X-Telegram-Bot-Api-Secret-Token 中原样带回该值
     */
    public Result<JSONObject> setWebhook(String token, String url, String secretToken) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("url", url);
        // secret_token 为空时不传，让 Telegram 侧不启用该回调密钥校验。
        if (secretToken != null && !secretToken.isBlank()) {
            body.put("secret_token", secretToken);
        }
        try {
            String resp = client().post()
                    .uri("/bot" + token + "/setWebhook")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);
            return result(resp, "Telegram webhook set successfully");
        } catch (Exception e) {
            log.warn("Telegram setWebhook failed: {}", e.getMessage());
            return fail(e);
        }
    }

    /**
     * 发送普通文本消息。
     *
     * @param parseMode HTML/MarkdownV2/NONE；NONE 表示不传 parse_mode
     */
    public Result<JSONObject> sendMessage(String token, Long chatId, String text, String parseMode) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("chat_id", chatId);
        body.put("text", text);
        String normalizedParseMode = TgConstants.ParseMode.normalize(parseMode);
        // Telegram 不认识 NONE，所以 NONE 仅作为系统内部“不要格式化”的语义。
        if (!TgConstants.ParseMode.isNone(normalizedParseMode)) {
            body.put("parse_mode", normalizedParseMode);
        }
        try {
            String resp = client().post()
                    .uri("/bot" + token + "/sendMessage")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);
            return result(resp, "Telegram message sent successfully");
        } catch (Exception e) {
            log.warn("Telegram sendMessage failed: {}", e.getMessage());
            return fail(e);
        }
    }

    /**
     * 解析 Telegram 标准响应，并把 ok=false 的业务错误转换为失败 Result。
     */
    Result<JSONObject> result(String resp, String successMessage) {
        try {
            JSONObject root = JSON.parseObject(resp);
            if (root == null) {
                return Result.fail("Telegram API response is empty");
            }
            if (!Boolean.TRUE.equals(root.getBoolean("ok"))) {
                return Result.fail(failureMessage(root));
            }
            return Result.success(root.getJSONObject("result"), successMessage);
        } catch (Exception e) {
            return Result.fail("Failed to parse Telegram API response: {}", e.getMessage());
        }
    }

    /**
     * 统一处理 HTTP 层异常；如果 Telegram 返回了 JSON 错误体，则复用 {@link #result(String, String)} 提取错误描述。
     */
    private Result<JSONObject> fail(Exception e) {
        if (e instanceof RestClientResponseException responseException) {
            String body = responseException.getResponseBodyAsString();
            if (body != null && !body.isBlank()) {
                return result(body, "");
            }
        }
        return Result.fail("Telegram API call failed: {}", e.getMessage());
    }

    /**
     * 组装 Telegram ok=false 时的错误信息，优先保留 error_code 和 description。
     */
    private String failureMessage(JSONObject root) {
        Integer errorCode = root.getInteger("error_code");
        String description = root.getString("description");
        if (description == null || description.isBlank()) {
            description = "Unknown error";
        }
        if (errorCode == null) {
            return "Telegram API call failed: " + description;
        }
        return "Telegram API call failed(" + errorCode + "): " + description;
    }
}
