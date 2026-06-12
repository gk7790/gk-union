package com.gk.merchant.enums;

import com.gk.common.enums.StringCodeEnum;

/**
 * 商户结算周期。
 */
public enum MerchantSettleCycleEnum implements StringCodeEnum {
    T0("T0", "T+0", "enum.merchantSettleCycle.t0"),
    T1("T1", "T+1", "enum.merchantSettleCycle.t1"),
    TN("TN", "T+N", "enum.merchantSettleCycle.tn");

    private final String code;
    private final String label;
    private final String i18nKey;

    MerchantSettleCycleEnum(String code, String label, String i18nKey) {
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
