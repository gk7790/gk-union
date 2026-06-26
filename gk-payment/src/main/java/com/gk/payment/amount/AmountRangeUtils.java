package com.gk.payment.amount;

import java.math.BigDecimal;

public final class AmountRangeUtils {
    public static final BigDecimal ZERO = BigDecimal.ZERO;

    private AmountRangeUtils() {
    }

    public static BigDecimal effectiveMin(BigDecimal minAmount) {
        return minAmount == null || minAmount.compareTo(ZERO) <= 0 ? ZERO : minAmount;
    }

    public static BigDecimal effectiveMax(BigDecimal maxAmount) {
        return isUnlimitedMax(maxAmount) ? null : maxAmount;
    }

    public static boolean isUnlimitedMax(BigDecimal maxAmount) {
        return maxAmount == null || maxAmount.compareTo(ZERO) <= 0;
    }

    public static boolean contains(BigDecimal minAmount, BigDecimal maxAmount, BigDecimal amount) {
        if (amount == null) {
            return false;
        }
        return amount.compareTo(effectiveMin(minAmount)) >= 0
                && (isUnlimitedMax(maxAmount) || amount.compareTo(maxAmount) <= 0);
    }

}
