package com.gk.infra.telegram;

import org.apache.commons.lang3.StringUtils;

/**
 * Telegram 绑定码用途。
 */
public enum TgBindPurpose {
    /** /bind: 绑定当前 Telegram 用户到系统商户主体。 */
    ACCOUNT,
    /** /merchant: 绑定商户主通知账号。 */
    MERCHANT_NOTIFY,
    /** /bindchat: 绑定当前 Telegram 群作为商户通知群。 */
    CHAT_NOTIFY;

    public static TgBindPurpose parse(String value) {
        if (StringUtils.isBlank(value)) {
            return ACCOUNT;
        }
        for (TgBindPurpose purpose : values()) {
            if (purpose.name().equalsIgnoreCase(value.trim())) {
                return purpose;
            }
        }
        throw new IllegalArgumentException("Unsupported Telegram bind purpose: " + value);
    }
}
