package com.gk.common.enums;

import com.gk.common.annotation.EnumDict;

@EnumDict("bizType")
public enum BizTypeEnum implements StringCodeEnum {
    PAY_ORDER("PAY_ORDER", "代收订单", "enum.bizType.payOrder"),
    PAYOUT_ORDER("PAYOUT_ORDER", "代付订单", "enum.bizType.payoutOrder"),
    MERCHANT_BALANCE_ADJUST("MERCHANT_BALANCE_ADJUST", "商户余额调整单", "enum.bizType.merchantBalanceAdjust");

    private final String code;
    private final String label;
    private final String i18nKey;

    BizTypeEnum(String code, String label, String i18nKey) {
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
