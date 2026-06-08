package com.gk.payment.fee;

import com.gk.payment.entity.MerchantFeeRuleEntity;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MerchantFeeCalculatorTest {

    @Test
    void calculatesRateFeeWithMerchantBearerPayinSettlement() {
        MerchantFeeRuleEntity rule = new MerchantFeeRuleEntity();
        rule.setFeeMode("RATE");
        rule.setFeeRate(new BigDecimal("0.025"));
        rule.setFeeFixed(BigDecimal.ZERO);
        rule.setFeeBearer("MERCHANT");

        MerchantFeeAmount amount = MerchantFeeCalculator.calculate(new BigDecimal("1000.00"), rule);

        assertEquals(new BigDecimal("25.00000000"), amount.feeAmount());
        assertEquals(new BigDecimal("975.00000000"), amount.payinSettleAmount());
    }

    @Test
    void calculatesRateFixedFeeWithMinAndMaxLimits() {
        MerchantFeeRuleEntity minRule = new MerchantFeeRuleEntity();
        minRule.setFeeMode("RATE_FIXED");
        minRule.setFeeRate(new BigDecimal("0.001"));
        minRule.setFeeFixed(new BigDecimal("1.00"));
        minRule.setMinFee(new BigDecimal("5.00"));
        minRule.setFeeBearer("MERCHANT");

        MerchantFeeAmount minAmount = MerchantFeeCalculator.calculate(new BigDecimal("100.00"), minRule);
        assertEquals(new BigDecimal("5.00000000"), minAmount.feeAmount());
        assertEquals(new BigDecimal("95.00000000"), minAmount.payinSettleAmount());

        MerchantFeeRuleEntity maxRule = new MerchantFeeRuleEntity();
        maxRule.setFeeMode("RATE");
        maxRule.setFeeRate(new BigDecimal("0.100"));
        maxRule.setMaxFee(new BigDecimal("20.00"));
        maxRule.setFeeBearer("MERCHANT");

        MerchantFeeAmount maxAmount = MerchantFeeCalculator.calculate(new BigDecimal("1000.00"), maxRule);
        assertEquals(new BigDecimal("20.00000000"), maxAmount.feeAmount());
        assertEquals(new BigDecimal("980.00000000"), maxAmount.payinSettleAmount());
    }

    @Test
    void keepsPayinSettlementAmountWhenCustomerBearsFee() {
        MerchantFeeRuleEntity rule = new MerchantFeeRuleEntity();
        rule.setFeeMode("FIXED");
        rule.setFeeFixed(new BigDecimal("8.00"));
        rule.setFeeBearer("CUSTOMER");

        MerchantFeeAmount amount = MerchantFeeCalculator.calculate(new BigDecimal("1000.00"), rule);

        assertEquals(new BigDecimal("8.00000000"), amount.feeAmount());
        assertEquals(new BigDecimal("1000.00000000"), amount.payinSettleAmount());
    }

    @Test
    void rejectsMerchantBearerFeeGreaterThanAmount() {
        MerchantFeeRuleEntity rule = new MerchantFeeRuleEntity();
        rule.setFeeMode("FIXED");
        rule.setFeeFixed(new BigDecimal("20.00"));
        rule.setFeeBearer("MERCHANT");

        assertThrows(IllegalArgumentException.class, () -> MerchantFeeCalculator.calculate(new BigDecimal("10.00"), rule));
    }
}
