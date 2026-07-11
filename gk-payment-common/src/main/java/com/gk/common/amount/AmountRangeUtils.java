package com.gk.common.amount;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

import java.math.BigDecimal;

/**
 * Amount range helpers for payment configuration.
 *
 * <p>Config semantics:
 * <ul>
 *   <li>min_amount = 0 means the range starts from zero.</li>
 *   <li>max_amount = 0 means no upper limit.</li>
 *   <li>negative values are invalid.</li>
 *   <li>Positive max_amount is inclusive.</li>
 * </ul>
 */
public final class AmountRangeUtils {
    public static final BigDecimal ZERO = BigDecimal.ZERO;

    private AmountRangeUtils() {
    }

    public static BigDecimal normalizeConfigAmount(BigDecimal amount) {
        return amount == null ? ZERO : amount;
    }

    public static BigDecimal effectiveMin(BigDecimal minAmount) {
        return hasMinLimit(minAmount) ? normalizeConfigAmount(minAmount) : ZERO;
    }

    public static BigDecimal effectiveMax(BigDecimal maxAmount) {
        return isUnlimitedMax(maxAmount) ? null : normalizeConfigAmount(maxAmount);
    }

    public static boolean hasMinLimit(BigDecimal minAmount) {
        return normalizeConfigAmount(minAmount).compareTo(ZERO) > 0;
    }

    public static boolean hasMaxLimit(BigDecimal maxAmount) {
        return !isUnlimitedMax(maxAmount);
    }

    public static boolean isUnlimitedMax(BigDecimal maxAmount) {
        return maxAmount == null || normalizeConfigAmount(maxAmount).compareTo(ZERO) == 0;
    }

    public static boolean isValidPublishRange(BigDecimal minAmount, BigDecimal maxAmount) {
        BigDecimal min = normalizeConfigAmount(minAmount);
        BigDecimal max = normalizeConfigAmount(maxAmount);
        if (min.compareTo(ZERO) < 0 || max.compareTo(ZERO) <= 0) {
            return false;
        }
        return min.compareTo(max) <= 0;
    }

    public static boolean contains(BigDecimal minAmount, BigDecimal maxAmount, BigDecimal amount) {
        if (amount == null || amount.compareTo(ZERO) < 0 || isInvalidConfigRange(minAmount, maxAmount)) {
            return false;
        }
        BigDecimal min = normalizeConfigAmount(minAmount);
        BigDecimal max = normalizeConfigAmount(maxAmount);
        return (!hasMinLimit(min) || amount.compareTo(min) >= 0)
                && (isUnlimitedMax(maxAmount) || amount.compareTo(max) <= 0);
    }

    public static String invalidConfigRangeMessage(BigDecimal minAmount, BigDecimal maxAmount) {
        BigDecimal min = normalizeConfigAmount(minAmount);
        BigDecimal max = normalizeConfigAmount(maxAmount);
        if (min.compareTo(ZERO) < 0 || max.compareTo(ZERO) < 0) {
            return "amount cannot be negative";
        }
        if (hasMaxLimit(maxAmount) && min.compareTo(max) > 0) {
            return "min_amount cannot be greater than max_amount";
        }
        return "amount range is invalid";
    }

    public static void validateConfigRange(BigDecimal minAmount, BigDecimal maxAmount) {
        BigDecimal min = normalizeConfigAmount(minAmount);
        BigDecimal max = normalizeConfigAmount(maxAmount);
        if (isInvalidConfigRange(min, max)) {
            throw new IllegalArgumentException(invalidConfigRangeMessage(min, max));
        }
    }

    private static boolean isInvalidConfigRange(BigDecimal minAmount, BigDecimal maxAmount) {
        BigDecimal min = normalizeConfigAmount(minAmount);
        BigDecimal max = normalizeConfigAmount(maxAmount);
        return min.compareTo(ZERO) < 0
                || max.compareTo(ZERO) < 0
                || (hasMaxLimit(maxAmount) && min.compareTo(max) > 0);
    }

    public static <T> void appendMatchesAmount(QueryWrapper<T> wrapper, BigDecimal amount) {
        if (amount == null) {
            return;
        }
        if (amount.compareTo(ZERO) < 0) {
            wrapper.apply("1 = 0");
            return;
        }
        wrapper.and(w -> w.le("min_amount", amount)
                .and(max -> max.eq("max_amount", ZERO).or().ge("max_amount", amount))
                .ge("min_amount", ZERO)
                .ge("max_amount", ZERO));
    }

    public static <T> void appendOverlapsRange(QueryWrapper<T> wrapper, BigDecimal rangeMin, BigDecimal rangeMax) {
        BigDecimal min = normalizeConfigAmount(rangeMin);
        BigDecimal max = normalizeConfigAmount(rangeMax);
        if (min.compareTo(ZERO) < 0 || max.compareTo(ZERO) < 0 || min.compareTo(max) > 0) {
            wrapper.apply("1 = 0");
            return;
        }
        wrapper.and(w -> w.le("min_amount", max)
                .and(item -> item.eq("max_amount", ZERO).or().ge("max_amount", min))
                .ge("min_amount", ZERO)
                .ge("max_amount", ZERO));
    }
}
