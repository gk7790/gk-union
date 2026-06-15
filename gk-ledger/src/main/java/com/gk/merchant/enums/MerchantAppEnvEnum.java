package com.gk.merchant.enums;

import com.gk.common.annotation.EnumDict;
import com.gk.common.enums.StringCodeEnum;

@EnumDict("merchantAppEnv")
public enum MerchantAppEnvEnum implements StringCodeEnum {
    TEST("TEST", "测试环境", "enum.merchantAppEnv.test"),
    PROD("PROD", "正式环境", "enum.merchantAppEnv.prod");

    private final String code;
    private final String label;
    private final String i18nKey;

    MerchantAppEnvEnum(String code, String label, String i18nKey) {
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
