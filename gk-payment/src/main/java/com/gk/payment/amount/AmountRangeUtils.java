package com.gk.payment.amount;

import java.math.BigDecimal;

public final class AmountRangeUtils {
    public static final BigDecimal ZERO = com.gk.common.amount.AmountRangeUtils.ZERO;

    private AmountRangeUtils() {
    }

    public static BigDecimal effectiveMin(BigDecimal minAmount) {
        return com.gk.common.amount.AmountRangeUtils.effectiveMin(minAmount);
    }

    public static BigDecimal effectiveMax(BigDecimal maxAmount) {
        return com.gk.common.amount.AmountRangeUtils.effectiveMax(maxAmount);
    }

    public static boolean hasMinLimit(BigDecimal minAmount) {
        return com.gk.common.amount.AmountRangeUtils.hasMinLimit(minAmount);
    }

    public static boolean hasMaxLimit(BigDecimal maxAmount) {
        return com.gk.common.amount.AmountRangeUtils.hasMaxLimit(maxAmount);
    }

    public static boolean isUnlimitedMax(BigDecimal maxAmount) {
        return com.gk.common.amount.AmountRangeUtils.isUnlimitedMax(maxAmount);
    }

    public static boolean isValidConfigRange(BigDecimal minAmount, BigDecimal maxAmount) {
        return com.gk.common.amount.AmountRangeUtils.isValidConfigRange(minAmount, maxAmount);
    }

    public static boolean contains(BigDecimal minAmount, BigDecimal maxAmount, BigDecimal amount) {
        return com.gk.common.amount.AmountRangeUtils.contains(minAmount, maxAmount, amount);
    }

}
