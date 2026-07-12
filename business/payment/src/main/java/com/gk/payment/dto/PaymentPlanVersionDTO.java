package com.gk.payment.dto;

import lombok.Data;

import java.time.Instant;

@Data
public class PaymentPlanVersionDTO {
    private Long id;
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
    private Long createdBy;
    private Instant createdAt;
    private Long updatedBy;
    private Instant updatedAt;
}
