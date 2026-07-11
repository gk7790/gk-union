package com.gk.payment.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.gk.common.core.entity.SimpleEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.Instant;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("merchant_notify_record")
public class MerchantNotifyRecordEntity extends SimpleEntity {
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
}
