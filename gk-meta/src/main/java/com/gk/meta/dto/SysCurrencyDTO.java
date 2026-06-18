package com.gk.meta.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.Instant;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SysCurrencyDTO {
    private Long id;
    @Schema(description = "币种代码，如 PHP、IDR、VND、USD")
    private String currency;
    @Schema(description = "币种英文名称")
    private String currencyName;
    @Schema(description = "币种符号")
    private String currencySymbol;
    @Schema(description = "ISO 4217 数字代码")
    private String numericCode;
    @Schema(description = "小数位")
    private Integer minorUnit;
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
