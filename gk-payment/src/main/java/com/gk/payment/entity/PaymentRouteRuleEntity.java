package com.gk.payment.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.gk.common.core.entity.SimpleEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("payment_route_rule")
public class PaymentRouteRuleEntity extends SimpleEntity {
    private Long tenantId;
    private String ruleName;
    private Long merchantId;
    private Long merchantAppId;
    private String direction;
    private String countryCode;
    private String currency;
    private String methodCode;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    private Long groupId;
    private Integer priority;
    private Instant effectiveAt;
    private Instant expireAt;
    private Integer status;
    private String remark;
}
