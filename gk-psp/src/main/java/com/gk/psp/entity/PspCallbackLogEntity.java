package com.gk.psp.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.gk.common.core.entity.SimpleEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.Instant;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("psp_callback_log")
public class PspCallbackLogEntity extends SimpleEntity {
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
}
