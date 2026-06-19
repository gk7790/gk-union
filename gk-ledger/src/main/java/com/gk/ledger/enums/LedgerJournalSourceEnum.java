package com.gk.ledger.enums;

import com.gk.common.annotation.EnumDict;
import com.gk.common.annotation.Style;
import com.gk.common.enums.StringCodeEnum;
import com.gk.common.enums.StyleType;

/**
 * 分录来源类型。
 */
@EnumDict("ledgerJournalSource")
public enum LedgerJournalSourceEnum implements StringCodeEnum {
    @Style(StyleType.PRIMARY)
    ORDER("ORDER", "订单", "enum.ledgerJournalSource.order"),

    @Style(StyleType.SUCCESS)
    SETTLE("SETTLE", "结算", "enum.ledgerJournalSource.settle"),

    @Style(StyleType.INFO)
    RECON("RECON", "对账", "enum.ledgerJournalSource.recon"),

    @Style(StyleType.WARNING)
    MANUAL("MANUAL", "手工", "enum.ledgerJournalSource.manual"),

    @Style(StyleType.INFO)
    SYSTEM("SYSTEM", "系统", "enum.ledgerJournalSource.system");

    private final String code;
    private final String label;
    private final String i18nKey;

    LedgerJournalSourceEnum(String code, String label, String i18nKey) {
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
