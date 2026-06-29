package com.gk.payment.dto;

import lombok.Data;

import java.time.Instant;

@Data
public class OrderStatusLogDTO {
    private Long id;
    private Long tenantId;
    private Long merchantId;
    private String logNo;
    private String direction;
    private Long orderId;
    private String orderNo;
    private String fromStatus;
    private String toStatus;
    private String eventType;
    private String reason;
    private String operatorType;
    private String operatorId;
    private String requestId;
    private String traceId;
    private String metadataJson;
    private Long createdBy;
    private Instant createdAt;
    private Long updatedBy;
    private Instant updatedAt;
}
