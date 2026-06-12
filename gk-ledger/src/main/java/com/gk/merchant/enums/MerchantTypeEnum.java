package com.gk.merchant.enums;

import com.gk.common.enums.StringCodeEnum;

/**
 * 商户类型。
 */
public enum MerchantTypeEnum implements StringCodeEnum {
    COMPANY("COMPANY", "企业", "enum.merchantType.company"),
    PERSON("PERSON", "个人", "enum.merchantType.person");

    private final String code;
    private final String label;
    private final String i18nKey;

    MerchantTypeEnum(String code, String label, String i18nKey) {
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
