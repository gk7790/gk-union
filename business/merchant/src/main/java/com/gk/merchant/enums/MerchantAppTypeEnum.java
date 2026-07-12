package com.gk.merchant.enums;

import com.gk.common.annotation.EnumDict;
import com.gk.common.annotation.Style;
import com.gk.common.enums.StringCodeEnum;
import com.gk.common.enums.StyleType;

/**
 * 商户应用类型
 */
@EnumDict("merchantAppType")
public enum MerchantAppTypeEnum implements StringCodeEnum {
    @Style(StyleType.PRIMARY)
    API("API", "API", "enum.merchantAppType.api"),

    @Style(StyleType.INFO)
    ADMIN("ADMIN", "后台", "enum.merchantAppType.admin"),

    @Style(StyleType.WARNING)
    SYSTEM("SYSTEM", "系统", "enum.merchantAppType.system");

    private final String code;
    private final String label;
    private final String i18nKey;

    MerchantAppTypeEnum(String code, String label, String i18nKey) {
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
