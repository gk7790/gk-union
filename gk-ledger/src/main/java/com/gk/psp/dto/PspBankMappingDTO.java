package com.gk.psp.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.Instant;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PspBankMappingDTO {
    private Long id;
    @Schema(description = "PSP供应商ID")
    private Long pspId;
    @Schema(description = "PSP账户ID，可选")
    private Long pspAccountId;
    @Schema(description = "PSP方式ID，可选")
    private Long pspMethodId;
    @Schema(description = "国家代码")
    private String countryCode;
    @Schema(description = "币种")
    private String currency;
    @Schema(description = "平台标准银行ID")
    private Long bankId;
    @Schema(description = "PSP侧银行编码")
    private String pspBankCode;
    @Schema(description = "PSP侧银行名称")
    private String pspBankName;
    @Schema(description = "PSP侧银行简称")
    private String pspBankShortName;
    @Schema(description = "方向：PAYIN/PAYOUT，空表示通用")
    private String direction;
    @Schema(description = "状态：1启用 2暂停 3禁用")
    private Integer status;
    @Schema(description = "PSP额外配置JSON")
    private String extra;
    @Schema(description = "备注")
    private String remark;
    private Long createdBy;
    private Instant createdAt;
    private Long updatedBy;
    private Instant updatedAt;
}
