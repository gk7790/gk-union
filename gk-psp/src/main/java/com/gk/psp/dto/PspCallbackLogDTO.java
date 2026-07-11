package com.gk.psp.dto;

import lombok.Data;

import java.time.Instant;

@Data
public class PspCallbackLogDTO {
    private Long id;
    private Long tenantId;
    private Long merchantId;
    private Long pspId;
    private String pspCode;
    private String bizType;
    private Long bizId;
    private String bizNo;
    private String pspOrderNo;
    private String callbackType;
    private String callbackId;
    private String callbackKey;
    private String bodyHash;
    private String headersJson;
    private String bodyJson;
    private String rawBody;
    private String signature;
    private String verifyStatus;
    private String processStatus;
    private String errorMsg;
    private Instant receivedAt;
    private Instant processedAt;
    private String traceId;
    private Long createdBy;
    private Instant createdAt;
    private Long updatedBy;
    private Instant updatedAt;
}
