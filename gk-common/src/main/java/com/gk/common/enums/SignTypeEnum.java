package com.gk.common.enums;

import com.gk.common.annotation.EnumDict;
import com.gk.common.annotation.Style;
import com.gk.common.enums.StyleType;

/**
 * API 签名算法。
 */
@EnumDict("signType")
public enum SignTypeEnum implements StringCodeEnum {
    @Style(StyleType.SUCCESS)
    HMAC_SHA256("HMAC_SHA256", "HMAC-SHA256", "enum.signType.hmacSha256"),

    @Style(StyleType.WARNING)
    MD5("MD5", "MD5", "enum.signType.md5"),

    @Style(StyleType.PRIMARY)
    RSA2("RSA2", "RSA2", "enum.signType.rsa2");

    private final String code;
    private final String label;
    private final String i18nKey;

    SignTypeEnum(String code, String label, String i18nKey) {
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
