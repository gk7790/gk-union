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
    void formatsZeroDecimalCurrency() {
        assertEquals("12", ApiAmountUtils.formatCurrencyAmount(new BigDecimal("12.99000000"), "JPY"));
    }

    @Test
    void formatsThreeDecimalCurrency() {
        assertEquals("12.345", ApiAmountUtils.formatCurrencyAmount(new BigDecimal("12.34590000"), "KWD"));
    }

    @Test
    void formatsNullAmountAsZero() {
        assertEquals("0.00", ApiAmountUtils.formatCurrencyAmount(null, "USD"));
    }
}
