package com.gk.merchant.enums;

import com.gk.common.annotation.EnumDict;
import com.gk.common.annotation.Style;
import com.gk.common.enums.StringCodeEnum;
import com.gk.common.enums.StyleType;

/**
 * 商户结算模式
 */
@EnumDict("merchantSettleMode")
public enum MerchantSettleModeEnum implements StringCodeEnum {
    @Style(StyleType.WARNING)
    MANUAL("MANUAL", "手工", "enum.merchantSettleMode.manual"),

    @Style(StyleType.SUCCESS)
    AUTO("AUTO", "自动", "enum.merchantSettleMode.auto");

    private final String code;
    private final String label;
    private final String i18nKey;

    MerchantSettleModeEnum(String code, String label, String i18nKey) {
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
