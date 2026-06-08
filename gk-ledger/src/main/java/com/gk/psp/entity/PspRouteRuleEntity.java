package com.gk.psp.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.gk.common.core.entity.SimpleEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("psp_route_rule")
public class PspRouteRuleEntity extends SimpleEntity {
    private Long tenantId;
    private Long merchantId;
    private Long merchantAppId;
    private String countryCode;
    private String currency;
    private String methodCode;
    private String direction;
    private Long pspId;
    private Long pspMethodId;
    private Long pspMerchantId;
    private Integer priority;
    private Integer weight;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer status;
    private String remark;
}
