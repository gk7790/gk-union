package com.gk.adjustment.enums;

import com.gk.common.annotation.EnumDict;
import com.gk.common.annotation.Style;
import com.gk.common.enums.StringCodeEnum;
import com.gk.common.enums.StyleType;

@EnumDict("merchantBalanceAdjustStatus")
public enum MerchantBalanceAdjustStatusEnum implements StringCodeEnum {
    @Style(StyleType.INFO)
    CREATED("CREATED", "已创建", "enum.merchantBalanceAdjustStatus.created"),

    @Style(StyleType.SUCCESS)
    POSTED("POSTED", "已入", "enum.merchantBalanceAdjustStatus.posted"),

    @Style(StyleType.DANGER)
    FAILED("FAILED", "失败", "enum.merchantBalanceAdjustStatus.failed");

    private final String code;
    private final String label;
    private final String i18nKey;

    MerchantBalanceAdjustStatusEnum(String code, String label, String i18nKey) {
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
