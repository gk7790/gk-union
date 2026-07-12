package com.gk.payment.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.gk.common.core.entity.SimpleEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("payment_route_channel")
public class PaymentRouteChannelEntity extends SimpleEntity {
    private Long tenantId;
    private Long groupId;
    private Long pspId;
    private Long pspMethodId;
    private Long pspAccountId;
    private Long pspFeeRuleId;
    private Integer priority;
    private Integer weight;
    private Integer fallbackOrder;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    private Integer status;
    private String remark;
}
