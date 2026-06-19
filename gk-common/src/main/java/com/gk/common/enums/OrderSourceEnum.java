package com.gk.common.enums;

import com.gk.common.annotation.EnumDict;
import com.gk.common.annotation.Style;
import com.gk.common.enums.StyleType;

/**
 * 订单来源。
 */
@EnumDict("orderSource")
public enum OrderSourceEnum implements StringCodeEnum {
    @Style(StyleType.PRIMARY)
    API("API", "API", "enum.orderSource.api"),

    @Style(StyleType.INFO)
    ADMIN("ADMIN", "后台", "enum.orderSource.admin"),

    @Style(StyleType.WARNING)
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
