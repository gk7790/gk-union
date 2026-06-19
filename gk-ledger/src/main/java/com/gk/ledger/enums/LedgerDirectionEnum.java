package com.gk.ledger.enums;

import com.gk.common.annotation.EnumDict;
import com.gk.common.annotation.Style;
import com.gk.common.enums.StringCodeEnum;
import com.gk.common.enums.StyleType;

/**
 * 记账方向 / 账户余额方向。
 */
@EnumDict("ledgerDirection")
public enum LedgerDirectionEnum implements StringCodeEnum {
    @Style(StyleType.PRIMARY)
    DEBIT("DEBIT", "借方", "enum.ledgerDirection.debit"),

    @Style(StyleType.SUCCESS)
    CREDIT("CREDIT", "贷方", "enum.ledgerDirection.credit");

    private final String code;
    private final String label;
    private final String i18nKey;

    LedgerDirectionEnum(String code, String label, String i18nKey) {
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
