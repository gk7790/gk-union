package com.gk.payment.plan;

import com.gk.openapi.error.ApiErrorCode;
import com.gk.openapi.error.ApiException;
import com.gk.payment.entity.PayoutOrderEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class PayoutPlanServiceImpl implements PayoutPlanService {
    private final PaymentPlanResolver paymentPlanResolver;

    @Override
    public PayoutPlan resolve(PayoutOrderEntity order) {
        PaymentPlan paymentPlan = paymentPlanResolver.resolvePayout(order)
                .orElseThrow(() -> new ApiException(ApiErrorCode.UNSUPPORTED_METHOD, "ACTIVE payment plan is not published"));
        return toPayoutPlan(order, paymentPlan);
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
