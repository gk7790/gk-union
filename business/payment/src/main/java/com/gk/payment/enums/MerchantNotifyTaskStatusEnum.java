package com.gk.payment.enums;

import com.gk.common.annotation.EnumDict;
import com.gk.common.annotation.Style;
import com.gk.common.enums.StringCodeEnum;
import com.gk.common.enums.StyleType;

/**
 * 商户通知任务状态 */
@EnumDict("merchantNotifyTaskStatus")
public enum MerchantNotifyTaskStatusEnum implements StringCodeEnum {
    @Style(StyleType.INFO)
    INIT("INIT", "待发", "enum.merchantNotifyTaskStatus.init"),

    @Style(StyleType.PRIMARY)
    PROCESSING("PROCESSING", "发送中", "enum.merchantNotifyTaskStatus.processing"),

    @Style(StyleType.SUCCESS)
    SUCCESS("SUCCESS", "成功", "enum.merchantNotifyTaskStatus.success"),

    @Style(StyleType.DANGER)
    FAILED("FAILED", "失败", "enum.merchantNotifyTaskStatus.failed"),

    @Style(StyleType.DANGER)
    DEAD("DEAD", "死信", "enum.merchantNotifyTaskStatus.dead");

    private final String code;
    private final String label;
    private final String i18nKey;

    MerchantNotifyTaskStatusEnum(String code, String label, String i18nKey) {
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
}
