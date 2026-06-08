package com.gk.psp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.Instant;

@Data
public class PspCallbackLogDTO {
    private Long id;
    @Schema(title = "租户ID")
    private Long tenantId;
    @Schema(title = "商户ID")
    private Long merchantId;
    @Schema(title = "PSP ID")
    private Long pspId;
    @Schema(title = "PSP编码")
    private String pspCode;
    @Schema(title = "业务类型")
    private String bizType;
    @Schema(title = "业务ID")
    private Long bizId;
    @Schema(title = "业务编号")
    private String bizNo;
    @Schema(title = "PSP订单号")
    private String pspOrderNo;
    @Schema(title = "回调类型")
    private String callbackType;
    @Schema(title = "PSP回调ID")
    private String callbackId;
    @Schema(title = "回调幂等键")
    private String callbackKey;
    @Schema(title = "回调报文哈希")
    private String bodyHash;
    @Schema(title = "回调请求头JSON")
    private String headersJson;
    @Schema(title = "回调请求体JSON")
    private String bodyJson;
    @Schema(title = "原始回调体")
    private String rawBody;
    @Schema(title = "回调签名")
    private String signature;
    @Schema(title = "验签状态")
    private String verifyStatus;
    @Schema(title = "处理状态")
    private String processStatus;
    @Schema(title = "错误信息")
    private String errorMsg;
    @Schema(title = "接收时间")
    private Instant receivedAt;
    @Schema(title = "处理时间")
    private Instant processedAt;
    @Schema(title = "链路追踪ID")
    private String traceId;
    private Long createdBy;
    private Instant createdAt;
    private Long updatedBy;
    private Instant updatedAt;
}
