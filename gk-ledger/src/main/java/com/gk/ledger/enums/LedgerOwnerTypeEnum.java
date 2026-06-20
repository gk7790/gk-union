package com.gk.ledger.enums;

import com.gk.common.annotation.EnumDict;
import com.gk.common.annotation.Style;
import com.gk.common.enums.StringCodeEnum;
import com.gk.common.enums.StyleType;

/**
 * 账本资金主体类型。
 */
@EnumDict("ledgerOwnerType")
public enum LedgerOwnerTypeEnum implements StringCodeEnum {
    @Style(StyleType.PRIMARY)
    PLATFORM("PLATFORM", "平台", "enum.ledgerOwnerType.platform"),

    @Style(StyleType.SUCCESS)
    TENANT("TENANT", "租户", "enum.ledgerOwnerType.tenant"),

    @Style(StyleType.INFO)
    MERCHANT("MERCHANT", "商户", "enum.ledgerOwnerType.merchant"),

    @Style(StyleType.WARNING)
    PSP("PSP", "通道", "enum.ledgerOwnerType.psp"),

    @Style(StyleType.INFO)
    INTERNAL("INTERNAL", "内部户", "enum.ledgerOwnerType.internal");

    private final String code;
    private final String label;
    private final String i18nKey;

    LedgerOwnerTypeEnum(String code, String label, String i18nKey) {
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
