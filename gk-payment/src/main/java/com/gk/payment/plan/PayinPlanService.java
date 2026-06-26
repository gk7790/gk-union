package com.gk.payment.plan;

import com.gk.payment.dto.PayinConfigPrecheckRequest;
import com.gk.payment.dto.PayinConfigPrecheckResult;
import com.gk.payment.entity.PayOrderEntity;

public interface PayinPlanService {
    PayinPlan resolve(PayOrderEntity order);

    PayinConfigPrecheckResult precheck(PayinConfigPrecheckRequest request);

    void evictAll();
}
