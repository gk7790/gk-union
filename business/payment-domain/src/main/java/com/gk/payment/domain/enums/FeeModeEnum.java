package com.gk.payment.domain.enums;

import com.gk.common.enums.StringCodeEnum;

import com.gk.common.annotation.EnumDict;
import com.gk.common.annotation.Style;
import com.gk.common.enums.StyleType;

/**
 * 手续费计算模式。
 */
@EnumDict("feeMode")
public enum FeeModeEnum implements StringCodeEnum {
    @Style(StyleType.PRIMARY)
    RATE("RATE", "比例", "enum.feeMode.rate"),

    @Style(StyleType.INFO)
    FIXED("FIXED", "固定", "enum.feeMode.fixed"),

    @Style(StyleType.SUCCESS)
    RATE_FIXED("RATE_FIXED", "比例+固定", "enum.feeMode.rateFixed");

    private final String code;
    private final String label;
    private final String i18nKey;

    FeeModeEnum(String code, String label, String i18nKey) {
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
