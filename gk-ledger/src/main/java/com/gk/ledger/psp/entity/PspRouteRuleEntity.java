package com.gk.ledger.psp.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.gk.common.core.entity.SimpleEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("psp_route_rule")
public class PspRouteRuleEntity extends SimpleEntity {
    private Long tenantId;
    private Long merchantId;
    private String appId;
    private String countryCode;
    private String currency;
    private String methodCode;
    private String direction;
    private Long pspMethodId;
    private Integer priority;
    private Integer weight;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer status;
    private String remark;
}
