package com.gk.tenant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.Instant;

@Data
public class SysTenantDTO {
    private Long id;
    @Schema(title = "租户公司名称")
    private String name;
    @Schema(title = "编码")
    private String code;
    @Schema(title = "状态")
    private Integer status;
    @Schema(title = "域名")
    private String domain;
    @Schema(title = "货币")
    private String currency;
    @Schema(title = "时区")
    private String timezone;
    @Schema(title = "语言")
    private String lang;
    @Schema(title = "租户key")
    private String apiKey;
    @Schema(title = "私密密钥")
    private String apiSecret;
    @Schema(title = "备注")
    private String remark;
    @Schema(title = "创建者")
    private Long createdBy;
    @Schema(title = "创建时间")
    private Instant createdAt;
    @Schema(title = "修改者")
    private Long updatedBy;
    @Schema(title = "修改时间")
    private Instant updatedAt;
}
