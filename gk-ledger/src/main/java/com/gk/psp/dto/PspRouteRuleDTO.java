package com.gk.psp.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;

@Data
public class PspRouteRuleDTO {
    private Long id;
    private Long tenantId;
    private Long merchantId;
    private Long merchantAppId;
    private String routeName;
    private String routeMode;
    private String countryCode;
    private String currency;
    private String methodCode;
    private String direction;
    private Long pspId;
    private Long pspMethodId;
    private Long pspAccountId;
    private Integer priority;
    private Integer weight;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer status;
    private String remark;
    private Long createdBy;
    private Instant createdAt;
    private Long updatedBy;
    private Instant updatedAt;
}
