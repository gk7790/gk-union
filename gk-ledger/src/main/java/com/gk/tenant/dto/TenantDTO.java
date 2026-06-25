package com.gk.tenant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.Instant;

@Data
@Schema(title = "租户资料", description = "租户基础信息")
public class TenantDTO {
    @Schema(title = "租户ID", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;
    @Schema(title = "租户公司名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "Acme Payments")
    private String name;
    @Schema(title = "租户编码", description = "租户唯一业务编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "acme")
    private String code;
    @Schema(title = "状�?, description = "1正常 2暂停 3停用", example = "1")
    private Integer status;
    @Schema(title = "域名", description = "租户独立访问域名，可�?)
    private String domain;
    @Schema(title = "默认币种", example = "INR")
    private String currency;
    @Schema(title = "默认时区", example = "Asia/Kolkata")
    private String timezone;
    @Schema(title = "默认语言", example = "en-US")
    private String lang;
    @Schema(title = "备注")
    private String remark;
    @Schema(title = "创建�?, accessMode = Schema.AccessMode.READ_ONLY)
    private Long createdBy;
    @Schema(title = "创建时间", accessMode = Schema.AccessMode.READ_ONLY)
    private Instant createdAt;
    @Schema(title = "修改�?, accessMode = Schema.AccessMode.READ_ONLY)
    private Long updatedBy;
    @Schema(title = "修改时间", accessMode = Schema.AccessMode.READ_ONLY)
    private Instant updatedAt;
}
