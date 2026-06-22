package com.gk.payment.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.gk.common.core.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * Payment plan amount bucket.
 * <p>
 * Buckets use half-open ranges: start <= amount < end.
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("payment_plan_bucket")
public class PaymentPlanBucketEntity extends BaseEntity {
    private Long tenantId;
    private Long catalogId;
    private BigDecimal bucketStartAmount;
    private BigDecimal bucketEndAmount;
    private Long merchantFeeRuleId;
    private String merchantFeeSnapshotJson;
    private Integer sort;
}
