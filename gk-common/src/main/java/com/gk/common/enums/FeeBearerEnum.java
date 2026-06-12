package com.gk.common.enums;

/**
 * 手续费承担方。
 */
public enum FeeBearerEnum implements StringCodeEnum {
    MERCHANT("MERCHANT", "商户", "enum.feeBearer.merchant"),
    CUSTOMER("CUSTOMER", "客户", "enum.feeBearer.customer");

    private final String code;
    private final String label;
    private final String i18nKey;

    FeeBearerEnum(String code, String label, String i18nKey) {
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
