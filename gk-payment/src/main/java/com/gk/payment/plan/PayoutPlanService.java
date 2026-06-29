package com.gk.payment.plan;

import com.gk.payment.entity.PayoutOrderEntity;

public interface PayoutPlanService {
    PayoutPlan resolve(PayoutOrderEntity order);
}
