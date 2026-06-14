package com.gk.telegram.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Telegram 配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "telegram")
public class TgProperties {
    /** Bot Token 加密密钥(16/24/32位); 生产请用环境变量覆盖 */
    private String cryptoKey = "1234567890123456";
    /** Bot Token 加密盐值 */
    private String cryptoSalt = "gk-telegram";
    /** Telegram Bot API 基础地址 */
    private String apiBaseUrl = "https://api.telegram.org";
    /** 本服务对外基础地址(用于拼接 setWebhook 的回调URL), 如 https://pay.example.com */
    private String webhookBaseUrl = "";
    /** 商户 Telegram 绑定码有效期(分钟) */
    private int bindCodeTtlMinutes = 10;
}
