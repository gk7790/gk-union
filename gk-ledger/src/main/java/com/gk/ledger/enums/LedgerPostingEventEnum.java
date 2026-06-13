package com.gk.ledger.enums;

import com.gk.common.enums.StringCodeEnum;

public enum LedgerPostingEventEnum implements StringCodeEnum {
    PAY_SUCCESS("PAY_SUCCESS", "代收成功", "enum.ledgerPostingEvent.paySuccess"),
    SETTLE_RELEASE("SETTLE_RELEASE", "结算释放", "enum.ledgerPostingEvent.settleRelease"),
    PAYOUT_FREEZE("PAYOUT_FREEZE", "代付冻结", "enum.ledgerPostingEvent.payoutFreeze"),
    PAYOUT_SUCCESS("PAYOUT_SUCCESS", "代付成功", "enum.ledgerPostingEvent.payoutSuccess"),
    PAYOUT_FAILED("PAYOUT_FAILED", "代付失败", "enum.ledgerPostingEvent.payoutFailed"),
    MANUAL_RECHARGE("MANUAL_RECHARGE", "手工充值", "enum.ledgerPostingEvent.manualRecharge"),
    MANUAL_DEDUCT("MANUAL_DEDUCT", "手工扣减", "enum.ledgerPostingEvent.manualDeduct"),
    MANUAL_REVERSE("MANUAL_REVERSE", "手工冲正", "enum.ledgerPostingEvent.manualReverse"),
    MANUAL_SUPPLEMENT("MANUAL_SUPPLEMENT", "手工补账", "enum.ledgerPostingEvent.manualSupplement");

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
