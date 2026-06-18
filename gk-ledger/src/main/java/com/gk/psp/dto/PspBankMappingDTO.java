package com.gk.psp.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PspBankMappingDTO {

    @Schema(description = "系统银行ID，仅用于前端展示或调试，不作为保存主字段")
    private Long bankId;

    @Schema(description = "PSP银行映射ID，存在表示已配置，不存在表示未配置")
    private Long mappingId;

    @Schema(description = "PSP供应商ID")
    private Long pspId;

    @Schema(description = "国家代码")
    private String countryCode;

    @Schema(description = "币种")
    private String currency;

    @Schema(description = "平台标准银行编码")
    private String bankCode;

    @Schema(description = "平台标准银行名称")
    private String bankName;

    @Schema(description = "平台标准银行简称")
    private String bankShortName;

    @Schema(description = "PSP侧银行编码")
    private String pspBankCode;

    @Schema(description = "映射状态：1启用 2暂停 3禁用")
    private Integer status;

    @Schema(description = "备注")
    private String remark;
}
