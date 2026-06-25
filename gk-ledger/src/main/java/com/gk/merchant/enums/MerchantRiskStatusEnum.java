package com.gk.merchant.enums;

import com.gk.common.annotation.EnumDict;
import com.gk.common.annotation.Style;
import com.gk.common.enums.StringCodeEnum;
import com.gk.common.enums.StyleType;

/**
 * 商户风控状态�? */
@EnumDict("merchantRiskStatus")
public enum MerchantRiskStatusEnum implements StringCodeEnum {
    @Style(StyleType.SUCCESS)
    NORMAL("NORMAL", "正常", "enum.merchantRiskStatus.normal"),

    @Style(StyleType.WARNING)
    FROZEN("FROZEN", "冻结", "enum.merchantRiskStatus.frozen"),

    @Style(StyleType.DANGER)
    BLOCKED("BLOCKED", "封禁", "enum.merchantRiskStatus.blocked");

    private final String code;
    private final String label;
    private final String i18nKey;

    MerchantRiskStatusEnum(String code, String label, String i18nKey) {
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
