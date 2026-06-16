package com.gk.psp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.Instant;

@Data
public class PspAccountDTO {
    @Schema(title = "主键ID")
    private Long id;
    @Schema(title = "租户ID")
    private Long tenantId;
    @Schema(title = "PSP ID")
    private Long pspId;
    @Schema(title = "PSP账户号/商户号")
    private String pspAccountNo;
    @Schema(title = "PSP账户名称")
    private String pspAccountName;
    @Schema(title = "状态: 1正常 2暂停 3停用")
    private Integer status;
    @Schema(title = "密钥类型: HMAC/BASIC/TOKEN")
    private String secretType;
    @Schema(title = "PSP API Key")
    private String apiKey;
    @Schema(title = "PSP API Secret")
    private String apiSecret;
    @Schema(title = "回调验签密钥")
    private String callbackSecret;
    @Schema(title = "扩展配置JSON")
    private String configJson;
    @Schema(title = "备注")
    private String remark;
    @Schema(title = "创建人")
    private Long createdBy;
    @Schema(title = "创建时间")
    private Instant createdAt;
    @Schema(title = "更新人")
    private Long updatedBy;
    @Schema(title = "更新时间")
    private Instant updatedAt;
}
