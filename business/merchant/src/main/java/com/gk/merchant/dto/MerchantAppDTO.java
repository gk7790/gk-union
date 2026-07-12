package com.gk.merchant.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.Instant;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MerchantAppDTO {
    private Long id;
    @Schema(title = "租户ID")
    private Long tenantId;
    @Schema(title = "商户ID")
    private Long merchantId;
    @Schema(title = "商户应用ID")
    private String appId;
    @Schema(title = "应用名称")
    private String appName;
    @Schema(title = "应用类型")
    private String appType;
    @Schema(title = "应用环境")
    private String appEnv;
    @Schema(title = "状")
    private Integer status;
    @Schema(title = "签名类型")
    private String signType;
    @Schema(title = "加密类型")
    private String encryptType;
    @Schema(title = "API密钥；详创建/重置返回完整值，列表脱敏")
    private String apiSecret;
    @Schema(title = "密钥版本")
    private Integer secretVersion;
    @Schema(title = "密钥更新时间")
    private Instant secretUpdatedAt;
    @Schema(title = "默认异步通知地址")
    private String notifyUrl;
    @Schema(title = "默认同步跳转地址")
    private String returnUrl;
    @Schema(title = "接口限流QPS")
    private Integer rateLimitQps;
    @Schema(title = "nonce防重放有效秒")
    private Integer nonceTtlSeconds;
    @Schema(title = "备注")
    private String remark;
    @Schema(title = "创建人ID")
    private Long createdBy;
    @Schema(title = "创建时间")
    private Instant createdAt;
    @Schema(title = "更新人ID")
    private Long updatedBy;
    @Schema(title = "更新时间")
    private Instant updatedAt;
}
