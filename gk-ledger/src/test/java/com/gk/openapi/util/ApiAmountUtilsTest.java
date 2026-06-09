package com.gk.openapi.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApiAmountUtilsTest {

    @Test
    void formatsZeroScientificBigDecimalAsPlainCurrencyAmount() {
        assertEquals("0.00", ApiAmountUtils.formatCurrencyAmount(new BigDecimal("0E-8"), "USD"));
    }

    @Test
    void formatsFiatAmountWithTwoDecimalsByDefault() {
        assertEquals("12.34", ApiAmountUtils.formatCurrencyAmount(new BigDecimal("12.34999999"), "PHP"));
    }

    @Test
    void formatsEveryCurrencyWithTwoDecimals() {
        assertEquals("12.99", ApiAmountUtils.formatCurrencyAmount(new BigDecimal("12.99000000"), "JPY"));
    }

    @Test
    void truncatesExtraDecimalsToTwoPlaces() {
        assertEquals("12.34", ApiAmountUtils.formatCurrencyAmount(new BigDecimal("12.34590000"), "KWD"));
    }

    @Test
    void formatsNullAmountAsZero() {
        assertEquals("0.00", ApiAmountUtils.formatCurrencyAmount(null, "USD"));
    }
}
