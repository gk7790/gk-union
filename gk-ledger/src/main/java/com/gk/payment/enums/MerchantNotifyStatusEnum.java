package com.gk.payment.enums;

import com.gk.common.annotation.EnumDict;
import com.gk.common.enums.StringCodeEnum;

/**
 * 订单下游商户通知状态。
 */
@EnumDict("merchantNotifyStatus")
public enum MerchantNotifyStatusEnum implements StringCodeEnum {
    NONE("NONE", "无需通知", "enum.merchantNotifyStatus.none"),
    PENDING("PENDING", "通知中", "enum.merchantNotifyStatus.pending"),
    SUCCESS("SUCCESS", "通知成功", "enum.merchantNotifyStatus.success"),
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
