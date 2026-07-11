package com.gk.payment.plan.model;

import com.gk.payment.entity.PaymentPlanCatalogEntity;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Redis 中缓存的支付决策表 */
@Data
public class PaymentPlanCatalog implements Serializable {
    private PaymentPlanCatalogEntity catalog;
    private List<PaymentPlanBucket> buckets = new ArrayList<>();
}
