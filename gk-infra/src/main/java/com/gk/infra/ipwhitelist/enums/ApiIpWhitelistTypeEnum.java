package com.gk.infra.ipwhitelist.enums;

import com.gk.common.annotation.EnumDict;
import com.gk.common.enums.StringCodeEnum;

@EnumDict("apiIpWhitelistType")
public enum ApiIpWhitelistTypeEnum implements StringCodeEnum {
    MERCHANT_OPENAPI("MERCHANT_OPENAPI", "商户API", "enum.apiIpWhitelistType.merchantOpenapi");

    private final String code;
    private final String label;
    private final String i18nKey;

    ApiIpWhitelistTypeEnum(String code, String label, String i18nKey) {
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
