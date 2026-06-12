package com.gk.merchant.enums;

import com.gk.common.enums.StringCodeEnum;

/**
 * 商户结算模式。
 */
public enum MerchantSettleModeEnum implements StringCodeEnum {
    MANUAL("MANUAL", "手工", "enum.merchantSettleMode.manual"),
    AUTO("AUTO", "自动", "enum.merchantSettleMode.auto");

    private final String code;
    private final String label;
    private final String i18nKey;

    MerchantSettleModeEnum(String code, String label, String i18nKey) {
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
