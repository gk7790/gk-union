package com.gk.common.enums;

import com.gk.common.annotation.EnumDict;
import com.gk.common.annotation.Style;

@EnumDict("bizType")
public enum BizTypeEnum implements StringCodeEnum {
    @Style(StyleType.PRIMARY)
    PAY_ORDER("PAY_ORDER", "代收订单", "enum.bizType.payOrder"),

    @Style(StyleType.WARNING)
    PAYOUT_ORDER("PAYOUT_ORDER", "代付订单", "enum.bizType.payoutOrder"),

    @Style(StyleType.INFO)
    BALANCE_ADJUST("BALANCE_ADJUST", "余额调整单", "enum.bizType.balanceAdjust");

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
