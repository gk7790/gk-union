package com.gk.payment.plan;

import com.gk.payment.entity.PayoutOrderEntity;

import java.util.Set;

public interface PayoutPlanService {
    PayoutPlan resolve(PayoutOrderEntity order);

    PayoutPlan resolve(PayoutOrderEntity order,
                       Set<Long> disabledPspIds,
                       Set<Long> disabledAccountIds,
                       Set<Long> disabledRouteOptionIds);
}
