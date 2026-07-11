package com.gk.payment.domain.enums;

import com.gk.common.enums.StringCodeEnum;
import com.gk.common.enums.StyleType;

import com.gk.common.annotation.EnumDict;
import com.gk.common.annotation.Style;

/**
 * API 签名算法。
 */
@EnumDict("signType")
public enum SignTypeEnum implements StringCodeEnum {
    @Style(StyleType.SUCCESS)
    HMAC_SHA256("HMAC_SHA256", "HMAC-SHA256", "enum.signType.hmacSha256"),

    @Style(StyleType.WARNING)
    MD5("MD5", "MD5", "enum.signType.md5");

    private static final String DEFAULT_OPEN_API_SIGN_TYPE = HMAC_SHA256.code;

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

    /**
     * 当前 OpenAPI 签名基于商户应用 apiSecret 共享密钥实现，仅支持 HMAC-SHA256 和 MD5。
     */
    public static boolean isOpenApiSupported(String value) {
        return HMAC_SHA256.matches(value) || MD5.matches(value);
    }

    public static String normalizeOpenApiSignType(String value) {
        return isOpenApiSupported(value) ? value.trim().toUpperCase() : DEFAULT_OPEN_API_SIGN_TYPE;
    }
}
