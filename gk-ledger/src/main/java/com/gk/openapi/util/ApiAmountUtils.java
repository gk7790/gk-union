package com.gk.openapi.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class ApiAmountUtils {
    private static final int DEFAULT_SCALE = 2;

    private ApiAmountUtils() {
    }

    public static String formatCurrencyAmount(BigDecimal amount, String currency) {
        BigDecimal value = amount == null ? BigDecimal.ZERO : amount;
        return value.setScale(DEFAULT_SCALE, RoundingMode.DOWN).toPlainString();
    }
}
