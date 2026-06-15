package com.gk.telegram.alert;

/**
 * Telegram 预警事件类型。
 */
public enum TgAlertEventType {
    /** 系统错误，需要及时处理。 */
    SYSTEM_ERROR,
    /** 系统警告，需要关注但不一定立即失败。 */
    SYSTEM_WARN,
    /** 支付成功业务通知。 */
    PAY_SUCCESS,
    /** 代付成功业务通知。 */
    PAYOUT_SUCCESS,
    /** 代付失败业务通知。 */
    PAYOUT_FAILED,
    /** 风控预警。 */
    RISK_ALERT;

    public String code() {
        return name();
    }
}
