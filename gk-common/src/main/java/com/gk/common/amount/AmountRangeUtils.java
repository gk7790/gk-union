package com.gk.common.amount;

import java.math.BigDecimal;

public final class AmountRangeUtils {
    public static final BigDecimal ZERO = BigDecimal.ZERO;

    private AmountRangeUtils() {
    }

    public static BigDecimal effectiveMin(BigDecimal minAmount) {
        return hasMinLimit(minAmount) ? minAmount : ZERO;
    }

    public static BigDecimal effectiveMax(BigDecimal maxAmount) {
        return isUnlimitedMax(maxAmount) ? null : maxAmount;
    }

    public static boolean hasMinLimit(BigDecimal minAmount) {
        return minAmount != null && minAmount.compareTo(ZERO) > 0;
    }

    public static boolean hasMaxLimit(BigDecimal maxAmount) {
        return maxAmount != null && maxAmount.compareTo(ZERO) > 0;
    }

    public static boolean isUnlimitedMax(BigDecimal maxAmount) {
        return maxAmount == null || maxAmount.compareTo(ZERO) == 0;
    }

    public static boolean isValidConfigRange(BigDecimal minAmount, BigDecimal maxAmount) {
        if (minAmount != null && minAmount.compareTo(ZERO) < 0) {
            return false;
        }
        if (maxAmount != null && maxAmount.compareTo(ZERO) < 0) {
            return false;
        }
        return !hasMaxLimit(maxAmount) || effectiveMin(minAmount).compareTo(maxAmount) <= 0;
    }

    public static boolean contains(BigDecimal minAmount, BigDecimal maxAmount, BigDecimal amount) {
        if (amount == null || !isValidConfigRange(minAmount, maxAmount)) {
            return false;
        }
        return (!hasMinLimit(minAmount) || amount.compareTo(minAmount) >= 0)
                && (!hasMaxLimit(maxAmount) || amount.compareTo(maxAmount) <= 0);
    }
}
