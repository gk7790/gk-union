package com.gk.payment.plan.cache;

import com.gk.merchant.service.MerchantPaymentPlanCacheEvictor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MerchantPaymentPlanCacheEvictorImpl implements MerchantPaymentPlanCacheEvictor {

    private final PayinPlanCache payinPlanCache;
    private final PaymentPlanCacheService paymentPlanCacheService;

    @Override
    public void evictAll() {
        payinPlanCache.evictAll();
        paymentPlanCacheService.evictAll();
    }
}
