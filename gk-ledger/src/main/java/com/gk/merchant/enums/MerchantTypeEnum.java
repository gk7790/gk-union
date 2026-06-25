package com.gk.merchant.enums;

import com.gk.common.annotation.EnumDict;
import com.gk.common.annotation.Style;
import com.gk.common.enums.StringCodeEnum;
import com.gk.common.enums.StyleType;

/**
 * 商户类型�?
 */
@EnumDict("merchantType")
public enum MerchantTypeEnum implements StringCodeEnum {
    @Style(StyleType.PRIMARY)
    COMPANY("COMPANY", "企业", "enum.merchantType.company"),

    @Style(StyleType.INFO)
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
