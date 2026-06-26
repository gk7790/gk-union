package com.gk.psp.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PspMethodDTO {
    private Long id;
    private Long pspId;
    private String pspCode;
    private String methodCode;
    private String pspMethodCode;
    private String methodName;
    private String countryCode;
    private String currency;
    private String direction;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    private BigDecimal dailyLimit;
    private Integer status;
    private String configJson;
    private String remark;
    private Long createdBy;
    private Instant createdAt;
    private Long updatedBy;
    private Instant updatedAt;
}
