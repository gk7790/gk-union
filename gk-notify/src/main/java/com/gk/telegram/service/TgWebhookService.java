package com.gk.telegram.service;

import java.util.Map;

/**
 * Telegram入站Webhook处理服务
 */
public interface TgWebhookService {

    /**
     * 处理一条入站Update
     *
     * @param botNo       路径中的机器人编号
     * @param secretToken 请求头 X-Telegram-Bot-Api-Secret-Token
     * @param rawBody     Telegram原始Update JSON
     * @return 处理结果(是否鉴权通过 + 可选回复体)
     */
    Result handle(String botNo, String secretToken, String rawBody);

    /**
     * 处理结果
     *
     * @param authorized 是否通过机器人/密钥校验(false应返回403)
     * @param reply      可选回复体(Telegram webhook 直返 sendMessage), 无回复为null
     */
    record Result(boolean authorized, Map<String, Object> reply) {
        /**
         * 机器人不存在、停用或 secret_token 校验失败时返回未授权。
         */
        public static Result unauthorized() {
            return new Result(false, null);
        }

        /**
         * webhook 处理成功；reply 为空时表示无需回复 Telegram 消息。
         */
        public static Result ok(Map<String, Object> reply) {
            return new Result(true, reply);
        }
    }
}
