package com.gk.adjustment.enums;

import com.gk.common.enums.StringCodeEnum;

public enum MerchantBalanceAdjustStatusEnum implements StringCodeEnum {
    CREATED("CREATED", "已创建", "enum.merchantBalanceAdjustStatus.created"),
    POSTED("POSTED", "已入账", "enum.merchantBalanceAdjustStatus.posted"),
    FAILED("FAILED", "失败", "enum.merchantBalanceAdjustStatus.failed");

    private final String code;
    private final String label;
    private final String i18nKey;

    MerchantBalanceAdjustStatusEnum(String code, String label, String i18nKey) {
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
