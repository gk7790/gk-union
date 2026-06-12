package com.gk.ledger.enums;

import com.gk.common.enums.StringCodeEnum;

/**
 * 过账业务事件。
 */
public enum LedgerPostingEventEnum implements StringCodeEnum {
    PAY_SUCCESS("PAY_SUCCESS", "代收成功", "enum.ledgerPostingEvent.paySuccess"),
    PAYOUT_FREEZE("PAYOUT_FREEZE", "代付冻结", "enum.ledgerPostingEvent.payoutFreeze"),
    PAYOUT_SUCCESS("PAYOUT_SUCCESS", "代付成功", "enum.ledgerPostingEvent.payoutSuccess"),
    PAYOUT_FAILED("PAYOUT_FAILED", "代付失败", "enum.ledgerPostingEvent.payoutFailed");

    private final String code;
    private final String label;
    private final String i18nKey;

    LedgerPostingEventEnum(String code, String label, String i18nKey) {
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
