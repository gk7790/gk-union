package com.gk.psp.fee;

import com.gk.psp.entity.PspFeeRuleEntity;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PspFeeCalculatorTest {

    @Test
    void calculatesRatePspFee() {
        PspFeeRuleEntity rule = new PspFeeRuleEntity();
        rule.setFeeMode("RATE");
        rule.setFeeRate(new BigDecimal("0.012"));

        BigDecimal fee = PspFeeCalculator.calculate(new BigDecimal("1000.00"), rule);

        assertEquals(new BigDecimal("12.00000000"), fee);
    }

    @Test
    void calculatesRateFixedPspFeeWithMinAndMaxLimits() {
        PspFeeRuleEntity minRule = new PspFeeRuleEntity();
        minRule.setFeeMode("RATE_FIXED");
        minRule.setFeeRate(new BigDecimal("0.001"));
        minRule.setFeeFixed(new BigDecimal("1.00"));
        minRule.setMinFee(new BigDecimal("5.00"));

        assertEquals(new BigDecimal("5.00000000"), PspFeeCalculator.calculate(new BigDecimal("100.00"), minRule));

        PspFeeRuleEntity maxRule = new PspFeeRuleEntity();
        maxRule.setFeeMode("RATE");
        maxRule.setFeeRate(new BigDecimal("0.100"));
        maxRule.setMaxFee(new BigDecimal("20.00"));

        assertEquals(new BigDecimal("20.00000000"), PspFeeCalculator.calculate(new BigDecimal("1000.00"), maxRule));
    }

    @Test
    void calculatesFixedPspFee() {
        PspFeeRuleEntity rule = new PspFeeRuleEntity();
        rule.setFeeMode("FIXED");
        rule.setFeeFixed(new BigDecimal("8.00"));

        assertEquals(new BigDecimal("8.00000000"), PspFeeCalculator.calculate(new BigDecimal("1000.00"), rule));
    }
}
