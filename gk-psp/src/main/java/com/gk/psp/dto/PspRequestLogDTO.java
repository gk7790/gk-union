package com.gk.psp.dto;

import lombok.Data;

import java.time.Instant;

@Data
public class PspRequestLogDTO {
    private Long id;
    private Long tenantId;
    private Long merchantId;
    private Long pspId;
    private String pspCode;
    private String bizType;
    private Long bizId;
    private String bizNo;
    private String requestNo;
    private String pspRequestNo;
    private String pspOrderNo;
    private String requestUrl;
    private String httpMethod;
    private String requestHeadersJson;
    private String requestBody;
    private Integer responseStatus;
    private String responseBody;
    private Integer success;
    private String errorCode;
    private String errorMsg;
    private Long costMs;
    private String traceId;
    private Long createdBy;
    private Instant createdAt;
    private Long updatedBy;
    private Instant updatedAt;
}
