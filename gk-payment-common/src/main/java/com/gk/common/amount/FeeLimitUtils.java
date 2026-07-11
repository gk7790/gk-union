package com.gk.common.amount;

import java.math.BigDecimal;

public final class FeeLimitUtils {
    private FeeLimitUtils() {
    }

    public static BigDecimal normalizeFeeLimit(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    public static boolean hasMinFee(BigDecimal minFee) {
        return normalizeFeeLimit(minFee).compareTo(BigDecimal.ZERO) > 0;
    }

    public static boolean hasMaxFee(BigDecimal maxFee) {
        return normalizeFeeLimit(maxFee).compareTo(BigDecimal.ZERO) > 0;
    }

    public static BigDecimal apply(BigDecimal fee, BigDecimal minFee, BigDecimal maxFee) {
        if (fee == null) {
            return BigDecimal.ZERO;
        }
        if (fee.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("fee cannot be negative");
        }
        if (isInvalidFeeLimit(minFee, maxFee)) {
            throw new IllegalArgumentException(invalidFeeLimitMessage(minFee, maxFee));
        }
        BigDecimal result = fee;
        BigDecimal min = normalizeFeeLimit(minFee);
        BigDecimal max = normalizeFeeLimit(maxFee);
        if (hasMinFee(minFee) && result.compareTo(min) < 0) {
            result = min;
        }
        if (hasMaxFee(maxFee) && result.compareTo(max) > 0) {
            result = max;
        }
        return result;
    }

    public static void validateFeeLimit(BigDecimal minFee, BigDecimal maxFee) {
        BigDecimal min = normalizeFeeLimit(minFee);
        BigDecimal max = normalizeFeeLimit(maxFee);
        if (isInvalidFeeLimit(min, max)) {
            throw new IllegalArgumentException(invalidFeeLimitMessage(min, max));
        }
    }

    private static boolean isInvalidFeeLimit(BigDecimal minFee, BigDecimal maxFee) {
        BigDecimal min = normalizeFeeLimit(minFee);
        BigDecimal max = normalizeFeeLimit(maxFee);
        return min.compareTo(BigDecimal.ZERO) < 0
                || max.compareTo(BigDecimal.ZERO) < 0
                || (hasMaxFee(maxFee) && min.compareTo(max) > 0);
    }

    public static String invalidFeeLimitMessage(BigDecimal minFee, BigDecimal maxFee) {
        BigDecimal min = normalizeFeeLimit(minFee);
        BigDecimal max = normalizeFeeLimit(maxFee);
        if (min.compareTo(BigDecimal.ZERO) < 0 || max.compareTo(BigDecimal.ZERO) < 0) {
            return "fee limit cannot be negative";
        }
        if (hasMaxFee(maxFee) && min.compareTo(max) > 0) {
            return "min_fee cannot be greater than max_fee";
        }
        return "fee limit is invalid";
    }
}
