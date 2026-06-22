package com.gk.payment.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.gk.common.core.entity.SimpleEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.Instant;

/**
 * 支付决策表头。
 * <p>
 * 后台发布配置时生成，商户 API 运行时只读取 ACTIVE 版本，不直接参与配置编译。
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("payment_plan_catalog")
public class PaymentPlanCatalogEntity extends SimpleEntity {
    private Long tenantId;
    private Long merchantId;
    private Long merchantAppId;
    private String direction;
    private String countryCode;
    private String currency;
    private String methodCode;
    private Long version;
    private String status;
    private Integer bucketCount;
    private Integer routeOptionCount;
    private String configHash;
    private Instant compiledAt;
    private Instant activatedAt;
    private String remark;
}
