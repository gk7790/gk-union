package com.gk.payment.domain.enums;

import com.gk.common.enums.StringCodeEnum;
import com.gk.common.enums.StyleType;

import com.gk.common.annotation.EnumDict;
import com.gk.common.annotation.Style;

@EnumDict("bizType")
public enum BizTypeEnum implements StringCodeEnum {
    @Style(StyleType.PRIMARY)
    PAYIN_ORDER("PAYIN_ORDER", "代收订单", "enum.bizType.payinOrder"),

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
