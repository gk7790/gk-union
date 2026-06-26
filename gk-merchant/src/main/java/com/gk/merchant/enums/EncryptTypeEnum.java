package com.gk.merchant.enums;

import com.gk.common.annotation.EnumDict;
import com.gk.common.annotation.Style;
import com.gk.common.enums.StringCodeEnum;
import com.gk.common.enums.StyleType;

/**
 * 商户应用加密类型
 */
@EnumDict("encryptType")
public enum EncryptTypeEnum implements StringCodeEnum {
    @Style(StyleType.INFO)
    NONE("NONE", "", "enum.encryptType.none"),

    @Style(StyleType.PRIMARY)
    AES("AES", "AES", "enum.encryptType.aes"),

    @Style(StyleType.SUCCESS)
    RSA("RSA", "RSA", "enum.encryptType.rsa");

    private final String code;
    private final String label;
    private final String i18nKey;

    EncryptTypeEnum(String code, String label, String i18nKey) {
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
