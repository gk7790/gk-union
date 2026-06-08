package com.gk.openapi.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.Map;

public class ApiAmountUtils {
    private static final int DEFAULT_SCALE = 2;
    private static final Map<String, Integer> CURRENCY_SCALE = Map.ofEntries(
            Map.entry("BHD", 3),
            Map.entry("IQD", 3),
            Map.entry("JOD", 3),
            Map.entry("KWD", 3),
            Map.entry("OMR", 3),
            Map.entry("TND", 3),
            Map.entry("CLP", 0),
            Map.entry("JPY", 0),
            Map.entry("KRW", 0),
            Map.entry("VND", 0)
    );

    private ApiAmountUtils() {
    }

    public static String formatCurrencyAmount(BigDecimal amount, String currency) {
        int scale = CURRENCY_SCALE.getOrDefault(currency == null ? "" : currency.toUpperCase(Locale.ROOT), DEFAULT_SCALE);
        BigDecimal value = amount == null ? BigDecimal.ZERO : amount;
        return value.setScale(scale, RoundingMode.DOWN).toPlainString();
    }
}
