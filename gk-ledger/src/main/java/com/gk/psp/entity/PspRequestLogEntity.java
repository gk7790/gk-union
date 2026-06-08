package com.gk.psp.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.gk.common.core.entity.SimpleEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("psp_request_log")
public class PspRequestLogEntity extends SimpleEntity {
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
}
