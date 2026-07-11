package com.gk.infra.telegram;

import org.apache.commons.lang3.StringUtils;

/**
 * Telegram 绑定码用途。
 */
public enum TgBindPurpose {
    /** /bind: 绑定当前 Telegram 用户到系统主体。 */
    USER,
    /** /merchant: 绑定商户主账号。 */
    MERCHANT,
    /** /bindchat: 绑定当前 Telegram 会话/群到系统主体。 */
    CHAT;

    public static TgBindPurpose parse(String value) {
        if (StringUtils.isBlank(value)) {
            throw new IllegalArgumentException("Telegram bind purpose is required");
        }
        String normalized = value.trim();
        for (TgBindPurpose purpose : values()) {
            if (purpose.name().equalsIgnoreCase(normalized)) {
                return purpose;
            }
        }
        throw new IllegalArgumentException("Unsupported Telegram bind purpose: " + value);
    }
}
