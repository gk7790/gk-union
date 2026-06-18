package com.gk.tenant.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.Instant;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TenantCurrencyDTO {
    private Long id;
    @Schema(description = "租户ID")
    private Long tenantId;
    @Schema(description = "币种代码")
    private String currency;
    @Schema(description = "状态：1启用 2暂停 3禁用")
    private Integer status;
    @Schema(description = "排序")
    private Integer sort;
    @Schema(description = "备注")
    private String remark;
    private Long createdBy;
    private Instant createdAt;
    private Long updatedBy;
    private Instant updatedAt;
}
