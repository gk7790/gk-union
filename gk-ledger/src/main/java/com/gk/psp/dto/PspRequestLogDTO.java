package com.gk.psp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.Instant;

@Data
public class PspRequestLogDTO {
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
    @Schema(title = "平台请求编号")
    private String requestNo;
    @Schema(title = "PSP请求编号")
    private String pspRequestNo;
    @Schema(title = "PSP订单号")
    private String pspOrderNo;
    @Schema(title = "请求URL")
    private String requestUrl;
    @Schema(title = "HTTP方法")
    private String httpMethod;
    @Schema(title = "请求头JSON")
    private String requestHeadersJson;
    @Schema(title = "请求体")
    private String requestBody;
    @Schema(title = "HTTP响应状态码")
    private Integer responseStatus;
    @Schema(title = "响应体")
    private String responseBody;
    @Schema(title = "是否成功")
    private Integer success;
    @Schema(title = "错误码")
    private String errorCode;
    @Schema(title = "错误信息")
    private String errorMsg;
    @Schema(title = "耗时毫秒")
    private Long costMs;
    @Schema(title = "链路追踪ID")
    private String traceId;
    private Long createdBy;
    private Instant createdAt;
    private Long updatedBy;
    private Instant updatedAt;
}
