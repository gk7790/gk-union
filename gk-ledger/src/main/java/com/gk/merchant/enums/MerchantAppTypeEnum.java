package com.gk.merchant.enums;

import com.gk.common.enums.StringCodeEnum;

/**
 * 商户应用类型。
 */
public enum MerchantAppTypeEnum implements StringCodeEnum {
    API("API", "API", "enum.merchantAppType.api"),
    ADMIN("ADMIN", "后台", "enum.merchantAppType.admin"),
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
