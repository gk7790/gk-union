package com.gk.psp.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
public class PspFeeRuleDTO {
    private Long id;
    private Long tenantId;
    private Long pspId;
    private Long pspAccountId;
    private Long pspMethodId;
    private String pspMethodCode;
    private String ruleName;
    private String direction;
    private String countryCode;
    private String currency;
    private String methodCode;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    private String feeMode;
    private BigDecimal feeRate;
    private BigDecimal feeFixed;
    private BigDecimal minFee;
    private BigDecimal maxFee;
    private Integer priority;
    private Instant effectiveAt;
    private Instant expireAt;
    private Integer status;
    private String remark;
    private Long createdBy;
    private Instant createdAt;
    private Long updatedBy;
    private Instant updatedAt;
}
