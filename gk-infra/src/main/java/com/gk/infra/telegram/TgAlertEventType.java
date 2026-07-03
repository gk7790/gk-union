package com.gk.infra.telegram;

import com.gk.common.annotation.EnumDict;
import com.gk.common.annotation.Style;
import com.gk.common.enums.StringCodeEnum;
import com.gk.common.enums.StyleType;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Telegram 通知事件大类。
 * <p>
 * 这里保留少量稳定的大类，具体原因放在消息标题或正文里，
 * 避免事件枚举随着业务增长无限膨胀。
 */
@EnumDict(value = "tgAlertEventType", name = "Telegram通知事件类型")
public enum TgAlertEventType implements StringCodeEnum {
    /** 系统错误，需要及时处理。 */
    @Style(StyleType.DANGER)
    SYSTEM_ERROR("SYSTEM_ERROR", "系统错误", "enum.tgAlertEventType.systemError"),

    /** 系统警告、启动停止、配置异常等需要关注的事件。 */
    @Style(StyleType.WARNING)
    SYSTEM_WARN("SYSTEM_WARN", "系统警告", "enum.tgAlertEventType.systemWarn"),

    /** 风控提醒。 */
    @Style(StyleType.WARNING)
    RISK_WARN("RISK_WARN", "风控提醒", "enum.tgAlertEventType.riskWarn"),

    /** 代收相关通知。 */
    @Style(StyleType.SUCCESS)
    PAYIN_NOTICE("PAYIN_NOTICE", "代收通知", "enum.tgAlertEventType.payinNotice"),

    /** 代付相关通知。 */
    @Style(StyleType.SUCCESS)
    PAYOUT_NOTICE("PAYOUT_NOTICE", "代付通知", "enum.tgAlertEventType.payoutNotice"),

    /** 渠道可用、禁用、维护、余额不足等通知。 */
    @Style(StyleType.PRIMARY)
    CHANNEL_NOTICE("CHANNEL_NOTICE", "渠道通知", "enum.tgAlertEventType.channelNotice"),

    /** 商户活动、配置、审核、账户等通知。 */
    @Style(StyleType.INFO)
    MERCHANT_NOTICE("MERCHANT_NOTICE", "商户通知", "enum.tgAlertEventType.merchantNotice"),

    /** 订单异常、人工审核等通知。 */
    @Style(StyleType.PRIMARY)
    ORDER_NOTICE("ORDER_NOTICE", "订单通知", "enum.tgAlertEventType.orderNotice"),

    /** 四方回调、上游接口异常等通知。 */
    @Style(StyleType.WARNING)
    PSP_NOTICE("PSP_NOTICE", "四方通知", "enum.tgAlertEventType.pspNotice");

    private final String code;
    private final String label;
    private final String i18nKey;

    TgAlertEventType(String code, String label, String i18nKey) {
        this.code = code;
        this.label = label;
        this.i18nKey = i18nKey;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public String label() {
        return label;
    }

    @Override
    public String i18nKey() {
        return i18nKey;
    }

    /**
     * 商户群默认订阅事件。
     * <p>
     * 不包含 PSP_NOTICE，四方异常默认只给内部群手动配置。
     */
    public static String merchantDefaultEventTypes() {
        return join(PAYIN_NOTICE, PAYOUT_NOTICE, RISK_WARN, CHANNEL_NOTICE, MERCHANT_NOTICE, ORDER_NOTICE);
    }

    /**
     * 平台/租户内部群默认订阅事件。
     */
    public static String opsDefaultEventTypes() {
        return join(SYSTEM_ERROR, SYSTEM_WARN, RISK_WARN);
    }

    private static String join(TgAlertEventType... eventTypes) {
        return Arrays.stream(eventTypes)
                .map(TgAlertEventType::code)
                .collect(Collectors.joining(","));
    }
}
