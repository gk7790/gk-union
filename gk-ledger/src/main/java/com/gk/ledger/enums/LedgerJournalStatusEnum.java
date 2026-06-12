package com.gk.ledger.enums;

import com.gk.common.enums.StringCodeEnum;

/**
 * 分录状态。
 */
public enum LedgerJournalStatusEnum implements StringCodeEnum {
    POSTED("POSTED", "已过账", "enum.ledgerJournalStatus.posted"),
    REVERSED("REVERSED", "已冲正", "enum.ledgerJournalStatus.reversed");

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
