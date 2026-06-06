package com.gk.ledger.common.enums;

import java.util.Arrays;

public enum LedgerAccountTypeEnum {
    MERCHANT_AVAILABLE(BalanceSideEnum.CREDIT),
    MERCHANT_PAYIN_FROZEN(BalanceSideEnum.CREDIT),
    MERCHANT_PAYOUT_FROZEN(BalanceSideEnum.CREDIT),
    PLATFORM_IN_TRANSIT(BalanceSideEnum.DEBIT),
    PLATFORM_REVENUE(BalanceSideEnum.CREDIT),
    PLATFORM_ADJUSTMENT_EXPENSE(BalanceSideEnum.DEBIT),
    PLATFORM_ADJUSTMENT_INCOME(BalanceSideEnum.CREDIT),
    PSP_PAYABLE(BalanceSideEnum.CREDIT);

    private final BalanceSideEnum balanceSide;

    LedgerAccountTypeEnum(BalanceSideEnum balanceSide) {
        this.balanceSide = balanceSide;
    }

    public BalanceSideEnum balanceSide() {
        return balanceSide;
    }

    public static LedgerAccountTypeEnum fromCode(String code) {
        return Arrays.stream(values())
                .filter(item -> item.name().equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported ledger account type: " + code));
    }
}
