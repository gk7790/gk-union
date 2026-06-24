package com.gk.payment.plan;

import com.gk.payment.amount.AmountRangeUtils;

import java.math.BigDecimal;

/**
 * Payment plan amount bucket range.
 * <p>
 * Runtime buckets use half-open ranges: startAmount <= amount < endAmount.
 */
public record PaymentPlanAmountRange(BigDecimal startAmount, BigDecimal endAmount) {
    public static PaymentPlanAmountRange closed(BigDecimal minAmount, BigDecimal maxAmount) {
        return new PaymentPlanAmountRange(AmountRangeUtils.effectiveMin(minAmount), AmountRangeUtils.effectiveMax(maxAmount));
    }
}
