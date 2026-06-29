package com.gk.payment.plan;

import com.gk.payment.dto.PayinConfigPrecheckRequest;
import com.gk.payment.dto.PayinConfigPrecheckResult;
import com.gk.payment.entity.PayinOrderEntity;

public interface PayinPlanService {
    PayinPlan resolve(PayinOrderEntity order);

    PayinConfigPrecheckResult precheck(PayinConfigPrecheckRequest request);

    void evictAll();
}
