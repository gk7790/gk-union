package com.gk.psp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.Instant;

@Data
public class PspMerchantDTO {
    private Long id;
    @Schema(title = "租户ID")
    private Long tenantId;
    @Schema(title = "商户ID")
    private Long merchantId;
    @Schema(title = "商户作用域ID")
    private Long merchantScopeId;
    @Schema(title = "PSP ID")
    private Long pspId;
    @Schema(title = "PSP编码")
    private String pspCode;
    @Schema(title = "PSP商户号")
    private String pspMerchantNo;
    @Schema(title = "PSP商户名称")
    private String pspMerchantName;
    @Schema(title = "状态")
    private Integer status;
    @Schema(title = "密钥类型")
    private String secretType;
    @Schema(title = "PSP API Key")
    private String apiKey;
    @Schema(title = "PSP API Secret")
    private String apiSecret;
    @Schema(title = "平台侧商户私钥引用")
    private String merchantPrivateKeyRef;
    @Schema(title = "PSP公钥")
    private String pspPublicKey;
    @Schema(title = "回调验签密钥")
    private String callbackSecret;
    @Schema(title = "扩展配置JSON")
    private String configJson;
    @Schema(title = "备注")
    private String remark;
    private Long createdBy;
    private Instant createdAt;
    private Long updatedBy;
    private Instant updatedAt;
}
