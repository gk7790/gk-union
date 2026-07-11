package com.gk.telegram.alert;

import com.gk.infra.notify.NotifyEventCodes;

/** Telegram-supported event codes. Labels and enabled state come from dictionary tgAlertEventType. */
public final class TgNotifyEventCodes {
    public static final String SYSTEM_ERROR = NotifyEventCodes.SYSTEM_ERROR;
    public static final String SYSTEM_WARN = NotifyEventCodes.SYSTEM_WARN;
    public static final String RISK_WARN = "RISK_WARN";
    public static final String PAYIN_NOTICE = "PAYIN_NOTICE";
    public static final String PAYOUT_NOTICE = "PAYOUT_NOTICE";
    public static final String CHANNEL_NOTICE = "CHANNEL_NOTICE";
    public static final String MERCHANT_NOTICE = "MERCHANT_NOTICE";
    public static final String ORDER_NOTICE = "ORDER_NOTICE";
    public static final String PSP_NOTICE = "PSP_NOTICE";

    public static String merchantDefaults() {
        return String.join(",", PAYIN_NOTICE, PAYOUT_NOTICE, RISK_WARN,
                CHANNEL_NOTICE, MERCHANT_NOTICE, ORDER_NOTICE);
    }

    public static String opsDefaults() {
        return String.join(",", SYSTEM_ERROR, SYSTEM_WARN, RISK_WARN);
    }

    private TgNotifyEventCodes() {
    }
}
