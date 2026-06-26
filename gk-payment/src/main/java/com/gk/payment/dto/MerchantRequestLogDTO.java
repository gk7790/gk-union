package com.gk.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.Instant;

@Data
public class MerchantRequestLogDTO {
    @Schema(title = "主键ID")
    private Long id;
    @Schema(title = "租户ID")
    private Long tenantId;
    @Schema(title = "商户ID")
    private Long merchantId;
    @Schema(title = "商户号快")
    private String merchantNo;
    @Schema(title = "商户应用ID")
    private Long merchantAppId;
    @Schema(title = "商户应用ID快照")
    private String appId;
    @Schema(title = "请求日志")
    private String requestNo;
    @Schema(title = "接口路径")
    private String apiPath;
    @Schema(title = "接口名称")
    private String apiName;
    @Schema(title = "HTTP方法")
    private String httpMethod;
    @Schema(title = "客户端IP")
    private String clientIp;
    @Schema(title = "User-Agent")
    private String userAgent;
    @Schema(title = "请求体哈")
    private String requestBodyHash;
    @Schema(title = "请求体JSON，脱敏后保存")
    private String requestBodyJson;
    @Schema(title = "请求参数JSON，脱敏后保存")
    private String requestParamsJson;
    @Schema(title = "签名类型")
    private String signType;
    @Schema(title = "商户提交签名，脱敏后保存")
    private String signValue;
    @Schema(title = "验签结果: 0失败 1成功")
    private Integer signValid;
    @Schema(title = "商户提交时间")
    private String timestampValue;
    @Schema(title = "商户提交nonce")
    private String nonceValue;
    @Schema(title = "业务类型: PAY_ORDER/PAYOUT_ORDER/BALANCE/QUERY")
    private String bizType;
    @Schema(title = "平台业务单号")
    private String bizNo;
    @Schema(title = "商户订单")
    private String merchantOrderNo;
    @Schema(title = "响应")
    private String responseCode;
    @Schema(title = "响应消息")
    private String responseMessage;
    @Schema(title = "响应体JSON，脱敏或摘要后保")
    private String responseBodyJson;
    @Schema(title = "处理状 RECEIVED/SUCCESS/FAILED/REJECTED")
    private String status;
    @Schema(title = "错误")
    private String errorCode;
    @Schema(title = "错误信息")
    private String errorMessage;
    @Schema(title = "处理耗时毫秒")
    private Long costMs;
    @Schema(title = "链路追踪ID")
    private String traceId;
    @Schema(title = "创建")
    private Long createdBy;
    @Schema(title = "创建时间")
    private Instant createdAt;
}
