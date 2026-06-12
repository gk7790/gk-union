package com.gk.common.enums;

/**
 * 订单来源。
 */
public enum OrderSourceEnum implements StringCodeEnum {
    API("API", "API", "enum.orderSource.api"),
    ADMIN("ADMIN", "后台", "enum.orderSource.admin"),
    SYSTEM("SYSTEM", "系统", "enum.orderSource.system");

    private final String code;
    private final String label;
    private final String i18nKey;

    OrderSourceEnum(String code, String label, String i18nKey) {
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
