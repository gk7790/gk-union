package com.gk.payment.dto;

import lombok.Data;

import java.time.Instant;

@Data
public class MerchantNotifyRecordDTO {
    private Long id;
    private Long tenantId;
    private Long notifyTaskId;
    private String taskNo;
    private Integer attemptNo;
    private String notifyUrl;
    private String httpMethod;
    private String contentType;
    private String requestSignature;
    private String requestHeadersJson;
    private String requestBody;
    private String responseHeadersJson;
    private Integer responseStatus;
    private String responseBody;
    private Integer success;
    private String errorMsg;
    private Long costMs;
    private Instant startedAt;
    private Instant finishedAt;
    private String traceId;
    private Long createdBy;
    private Instant createdAt;
    private Long updatedBy;
    private Instant updatedAt;
}
