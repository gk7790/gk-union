package com.gk.merchant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.Instant;

@Data
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
    @Schema(title = "状态")
    private Integer status;
    @Schema(title = "签名类型")
    private String signType;
    @Schema(title = "加密类型")
    private String encryptType;
    @Schema(title = "API密钥；详情/创建/重置返回完整值，列表脱敏")
    private String apiSecret;
    @Schema(title = "密钥版本号")
    private Integer secretVersion;
    @Schema(title = "密钥更新时间")
    private Instant secretUpdatedAt;
    @Schema(title = "商户公钥")
    private String merchantPublicKey;
    @Schema(title = "平台公钥快照")
    private String platformPublicKey;
    @Schema(title = "默认异步通知地址")
    private String notifyUrl;
    @Schema(title = "默认同步跳转地址")
    private String returnUrl;
    @Schema(title = "IP白名单JSON数组")
    private String ipWhitelistJson;
    @Schema(title = "允许币种JSON数组")
    private String allowedCurrencyJson;
    @Schema(title = "允许支付方式JSON数组")
    private String allowedMethodJson;
    @Schema(title = "接口限流QPS")
    private Integer rateLimitQps;
    @Schema(title = "nonce防重放有效秒数")
    private Integer nonceTtlSeconds;
    @Schema(title = "应用扩展配置JSON")
    private String configJson;
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
