package com.gk.payment.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.gk.common.core.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 支付决策金额桶。
 * <p>
 * 每个 bucket 保存一段金额区间内已经命中的商户费率、PSP 路由和 PSP 成本费率。
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
