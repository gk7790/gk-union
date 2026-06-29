package com.gk.ledger.enums;

import com.gk.common.annotation.EnumDict;
import com.gk.common.annotation.Style;
import com.gk.common.enums.StringCodeEnum;
import com.gk.common.enums.StyleType;

@EnumDict("ledgerPostingEvent")
public enum LedgerPostingEventEnum implements StringCodeEnum {
    @Style(StyleType.SUCCESS)
    PAYIN_SUCCESS("PAYIN_SUCCESS", "代收成功", "enum.ledgerPostingEvent.payinSuccess"),

    @Style(StyleType.SUCCESS)
    SETTLE_RELEASE("SETTLE_RELEASE", "结算释放", "enum.ledgerPostingEvent.settleRelease"),

    @Style(StyleType.WARNING)
    PAYOUT_FREEZE("PAYOUT_FREEZE", "代付冻结", "enum.ledgerPostingEvent.payoutFreeze"),

    @Style(StyleType.SUCCESS)
    PAYOUT_SUCCESS("PAYOUT_SUCCESS", "代付成功", "enum.ledgerPostingEvent.payoutSuccess"),

    @Style(StyleType.DANGER)
    PAYOUT_FAILED("PAYOUT_FAILED", "代付失败", "enum.ledgerPostingEvent.payoutFailed"),

    @Style(StyleType.SUCCESS)
    MANUAL_RECHARGE("MANUAL_RECHARGE", "手工充", "enum.ledgerPostingEvent.manualRecharge"),

    @Style(StyleType.DANGER)
    MANUAL_DEDUCT("MANUAL_DEDUCT", "手工扣减", "enum.ledgerPostingEvent.manualDeduct"),

    @Style(StyleType.WARNING)
    MANUAL_REVERSE("MANUAL_REVERSE", "手工冲正", "enum.ledgerPostingEvent.manualReverse"),

    @Style(StyleType.INFO)
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
