package com.gk.common.enums;

import com.gk.common.annotation.EnumDict;

/**
 * 手续费计算模式。
 */
@EnumDict("feeMode")
public enum FeeModeEnum implements StringCodeEnum {
    RATE("RATE", "比例", "enum.feeMode.rate"),
    FIXED("FIXED", "固定", "enum.feeMode.fixed"),
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
