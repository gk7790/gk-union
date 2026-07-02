package com.gk.payment.plan.model;

import com.gk.payment.entity.PaymentPlanBucketEntity;
import com.gk.payment.entity.PaymentPlanRouteOptionEntity;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * ACTIVE plan 中的金额桶运行时对象 * <p>
 * DB bucket route option 分表保存，Redis 中按 bucket 内嵌候选路由整体缓存 */
@Data
public class PaymentPlanBucket implements Serializable {
    private PaymentPlanBucketEntity bucket;
    private List<PaymentPlanRouteOptionEntity> routeOptions = new ArrayList<>();
}
