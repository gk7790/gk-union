package com.gk.payment.plan;

import com.gk.common.amount.AmountRangeUtils;

import java.math.BigDecimal;

/**
 * 支付方案金额分段区间。
 * <p>
 * 运行时 bucket 使用半开区间 {@code [startAmount, endAmount)}；{@code endAmount == null} 表示无上限。
 */
public record PaymentPlanAmountRange(BigDecimal startAmount, BigDecimal endAmount) {
    public static PaymentPlanAmountRange closed(BigDecimal minAmount, BigDecimal maxAmount) {
        return new PaymentPlanAmountRange(AmountRangeUtils.effectiveMin(minAmount), AmountRangeUtils.effectiveMax(maxAmount));
    }
}
