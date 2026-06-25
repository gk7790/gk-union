package com.gk.ledger.enums;

import com.gk.common.annotation.EnumDict;
import com.gk.common.annotation.Style;
import com.gk.common.enums.StringCodeEnum;
import com.gk.common.enums.StyleType;

/**
 * 分录状态�? */
@EnumDict("ledgerJournalStatus")
public enum LedgerJournalStatusEnum implements StringCodeEnum {
    @Style(StyleType.SUCCESS)
    POSTED("POSTED", "已过�?, "enum.ledgerJournalStatus.posted"),

    @Style(StyleType.WARNING)
    REVERSED("REVERSED", "已冲�?, "enum.ledgerJournalStatus.reversed");

    private final String code;
    private final String label;
    private final String i18nKey;

    LedgerJournalStatusEnum(String code, String label, String i18nKey) {
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
