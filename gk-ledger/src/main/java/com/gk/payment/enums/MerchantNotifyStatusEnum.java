package com.gk.payment.enums;

import com.gk.common.annotation.EnumDict;
import com.gk.common.annotation.Style;
import com.gk.common.enums.StringCodeEnum;
import com.gk.common.enums.StyleType;

/**
 * 订单下游商户通知状态�? */
@EnumDict("merchantNotifyStatus")
public enum MerchantNotifyStatusEnum implements StringCodeEnum {
    @Style(StyleType.INFO)
    NONE("NONE", "无需通知", "enum.merchantNotifyStatus.none"),

    @Style(StyleType.PRIMARY)
    PENDING("PENDING", "通知�?, "enum.merchantNotifyStatus.pending"),

    @Style(StyleType.SUCCESS)
    SUCCESS("SUCCESS", "通知成功", "enum.merchantNotifyStatus.success"),

    @Style(StyleType.DANGER)
    FAILED("FAILED", "通知失败", "enum.merchantNotifyStatus.failed");

    private final String code;
    private final String label;
    private final String i18nKey;

    MerchantNotifyStatusEnum(String code, String label, String i18nKey) {
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
