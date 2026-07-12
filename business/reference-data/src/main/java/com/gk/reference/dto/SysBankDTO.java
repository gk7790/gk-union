package com.gk.reference.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.Instant;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SysBankDTO {
    private Long id;
    @Schema(description = "国家代码，如 PH、ID、VN")
    private String countryCode;
    @Schema(description = "币种，如 PHP、IDR、VND")
    private String currency;
    @Schema(description = "平台标准银行编码")
    private String bankCode;
    @Schema(description = "银行官方全称")
    private String bankName;
    @Schema(description = "银行简称")
    private String bankShortName;
    @Schema(description = "SWIFT/BIC")
    private String swiftCode;
    @Schema(description = "本地清算码")
    private String localClearingCode;
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
