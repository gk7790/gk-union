package com.gk.common.enums;

/**
 * API 签名算法。
 */
public enum SignTypeEnum implements StringCodeEnum {
    HMAC_SHA256("HMAC_SHA256", "HMAC-SHA256", "enum.signType.hmacSha256"),
    MD5("MD5", "MD5", "enum.signType.md5"),
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
