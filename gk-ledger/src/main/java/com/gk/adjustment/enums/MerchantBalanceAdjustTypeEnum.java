package com.gk.adjustment.enums;

import com.gk.common.annotation.EnumDict;
import com.gk.common.enums.StringCodeEnum;
import com.gk.ledger.enums.LedgerPostingEventEnum;

@EnumDict("merchantBalanceAdjustType")
public enum MerchantBalanceAdjustTypeEnum implements StringCodeEnum {
    RECHARGE("RECHARGE", "充值", "enum.merchantBalanceAdjustType.recharge", LedgerPostingEventEnum.MANUAL_RECHARGE.code(), true),
    DEDUCT("DEDUCT", "扣减", "enum.merchantBalanceAdjustType.deduct", LedgerPostingEventEnum.MANUAL_DEDUCT.code(), false),
    REVERSE("REVERSE", "冲正", "enum.merchantBalanceAdjustType.reverse", LedgerPostingEventEnum.MANUAL_REVERSE.code(), false),
    SUPPLEMENT("SUPPLEMENT", "补账", "enum.merchantBalanceAdjustType.supplement", LedgerPostingEventEnum.MANUAL_SUPPLEMENT.code(), true);

    private final String code;
    private final String label;
    private final String i18nKey;
    private final String eventType;
    private final boolean increase;

    MerchantBalanceAdjustTypeEnum(String code, String label, String i18nKey, String eventType, boolean increase) {
        this.code = code;
        this.label = label;
        this.i18nKey = i18nKey;
        this.eventType = eventType;
        this.increase = increase;
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

    public String eventType() {
        return eventType;
    }

    public boolean increase() {
        return increase;
    }
}
