package com.gk.payment.plan.impl;

import com.gk.payment.domain.error.PaymentErrorCode;
import com.gk.payment.domain.error.PaymentException;
import com.gk.payment.domain.error.PaymentExceptions;
import com.gk.payment.entity.PayoutOrderEntity;
import com.gk.payment.plan.PaymentPlanResolver;
import com.gk.payment.plan.PayoutPlanService;
import com.gk.payment.plan.model.PaymentPlan;
import com.gk.payment.plan.model.PayoutPlan;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PayoutPlanServiceImpl implements PayoutPlanService {
    private final PaymentPlanResolver paymentPlanResolver;

    @Override
    public PayoutPlan resolve(PayoutOrderEntity order) {
        return resolve(order, Set.of(), Set.of(), Set.of());
    }

    @Override
    public PayoutPlan resolve(PayoutOrderEntity order,
                              Set<Long> disabledPspIds,
                              Set<Long> disabledAccountIds,
                              Set<Long> disabledRouteOptionIds) {
        try {
            PaymentPlan paymentPlan = paymentPlanResolver.resolvePayout(order, disabledPspIds, disabledAccountIds, disabledRouteOptionIds)
                    .orElseThrow(() -> new PaymentException(PaymentErrorCode.UNSUPPORTED_METHOD, "ACTIVE payment plan is not published"));
            return toPayoutPlan(order, paymentPlan);
        } catch (IllegalArgumentException ex) {
            throw PaymentExceptions.normalize(ex);
        }
    }

    private PayoutPlan toPayoutPlan(PayoutOrderEntity order, PaymentPlan paymentPlan) {
        PayoutPlan plan = new PayoutPlan();
        plan.setCatalogId(paymentPlan.getCatalogId());
        plan.setCatalogVersion(paymentPlan.getCatalogVersion());
        plan.setBucketId(paymentPlan.getBucketId());
        plan.setRouteOptionId(paymentPlan.getRouteOptionId());
        plan.setMerchantFee(paymentPlan.getMerchantFee());
        plan.setRoute(paymentPlan.getRoute());
        plan.setPspFee(paymentPlan.getPspFee());
        plan.setMerchantFeeAmount(defaultZero(paymentPlan.getMerchantFeeAmount()));
        plan.setTotalDebitAmount(order.getAmount().add(plan.getMerchantFeeAmount()));
        plan.setPspFeeAmount(defaultZero(paymentPlan.getPspFeeAmount()));
        return plan;
    }

    private BigDecimal defaultZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
