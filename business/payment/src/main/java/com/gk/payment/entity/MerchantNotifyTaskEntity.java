package com.gk.payment.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.gk.common.core.entity.SimpleEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.Instant;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("merchant_notify_task")
public class MerchantNotifyTaskEntity extends SimpleEntity {
    private Long tenantId;
    private Long merchantId;
    private Long merchantAppId;
    private String appId;
    private String taskNo;
    private String bizType;
    private Long bizId;
    private String bizNo;
    private String eventType;
    private String sourceEventId;
    private String notifyUrl;
    private String httpMethod;
    private String contentType;
    private String charset;
    private String signType;
    private String signature;
    private String payloadHash;
    private String headersJson;
    private String payloadJson;
    private Integer timeoutMs;
    private String status;
    private Integer retryCount;
    private Integer maxRetryCount;
    private Instant nextRetryAt;
    private Integer lastHttpStatus;
    private String lastResponseBody;
    private String lastErrorMsg;
    private Instant lastAttemptAt;
    private String lockedBy;
    private Instant lockedAt;
    private Instant lockUntil;
    private Instant successAt;
    private Instant deadAt;
    private String traceId;
    private String remark;
}
