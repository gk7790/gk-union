package com.gk.tenant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class SysTenantDTO {
    @Schema(title = "id")
    private Long id;
    @Schema(title = "名称")
    private String name;
    @Schema(title = "编码")
    private String code;
    @Schema(title = "状态")
    private Integer status;
    @Schema(title = "类型")
    private Integer planType;
}
