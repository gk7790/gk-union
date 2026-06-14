package com.gk.telegram.bot;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.gk.common.constant.Constant;
import com.gk.common.model.Result;
import com.gk.infra.config.model.TgBaseConfig;
import com.gk.infra.config.service.SysParamsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Telegram Bot API client.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TgBotApiClient {
    private final SysParamsService sysParamsService;

    private RestClient client() {
        TgBaseConfig tgBase = sysParamsService.getValueObject(Constant.TELEGRAM_BASE_CONFIG_KEY, TgBaseConfig.class);
        String apiBaseUrl = tgBase == null ? null : tgBase.getApiBaseUrl();
        if (apiBaseUrl == null || apiBaseUrl.isBlank()) {
            apiBaseUrl = "https://api.telegram.org";
        }
        return RestClient.builder().baseUrl(apiBaseUrl.trim()).build();
    }

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

    public Result<JSONObject> setWebhook(String token, String url, String secretToken) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("url", url);
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

    public Result<JSONObject> sendMessage(String token, Long chatId, String text, String parseMode) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("chat_id", chatId);
        body.put("text", text);
        if (parseMode != null && !parseMode.isBlank() && !"NONE".equalsIgnoreCase(parseMode)) {
            body.put("parse_mode", parseMode);
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

    private Result<JSONObject> fail(Exception e) {
        if (e instanceof RestClientResponseException responseException) {
            String body = responseException.getResponseBodyAsString();
            if (body != null && !body.isBlank()) {
                return result(body, "");
            }
        }
        return Result.fail("Telegram API call failed: {}", e.getMessage());
    }

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
