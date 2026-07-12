package com.gk.telegram.bind;

import org.apache.commons.lang3.StringUtils;

/** Purpose of a one-time Telegram binding ticket. */
public enum TgBindPurpose {
    USER,
    MERCHANT,
    CHAT;

    public static TgBindPurpose parse(String value) {
        if (StringUtils.isBlank(value)) {
            throw new IllegalArgumentException("Telegram bind purpose is required");
        }
        for (TgBindPurpose purpose : values()) {
            if (purpose.name().equalsIgnoreCase(value.trim())) {
                return purpose;
            }
        }
        throw new IllegalArgumentException("Unsupported Telegram bind purpose: " + value);
    }
}
