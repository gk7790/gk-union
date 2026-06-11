package com.gk.telegram.bot;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.gk.telegram.config.TgProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Telegram Bot API 客户端(基于 Spring RestClient, 无需引入第三方SDK)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TgBotApiClient {
    private final TgProperties properties;

    private RestClient client() {
        return RestClient.builder().baseUrl(properties.getApiBaseUrl()).build();
    }

    /**
     * getMe: 校验token并获取机器人基础信息
     *
     * @return Telegram返回的 result 对象(含id/username), 失败返回null
     */
    public JSONObject getMe(String token) {
        try {
            String resp = client().get()
                    .uri("/bot" + token + "/getMe")
                    .retrieve()
                    .body(String.class);
            return result(resp);
        } catch (Exception e) {
            log.warn("Telegram getMe failed: {}", e.getMessage());
            return null;
        }
    }

    /**
     * setWebhook: 设置回调地址与 secret_token
     *
     * @return 是否成功
     */
    public boolean setWebhook(String token, String url, String secretToken) {
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
            JSONObject root = JSON.parseObject(resp);
            return root != null && Boolean.TRUE.equals(root.getBoolean("ok"));
        } catch (Exception e) {
            log.warn("Telegram setWebhook failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * sendMessage: 主动发送文本消息(出站推送使用)
     *
     * @return Telegram返回的 result 对象(含message_id), 失败返回null
     */
    public JSONObject sendMessage(String token, Long chatId, String text, String parseMode) {
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
            return result(resp);
        } catch (Exception e) {
            log.warn("Telegram sendMessage failed: {}", e.getMessage());
            return null;
        }
    }

    private JSONObject result(String resp) {
        JSONObject root = JSON.parseObject(resp);
        if (root == null || !Boolean.TRUE.equals(root.getBoolean("ok"))) {
            return null;
        }
        return root.getJSONObject("result");
    }
}
