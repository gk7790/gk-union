package com.gk.psp.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PspBankMappingDTO {
    private Long bankId;
    private Long mappingId;
    private Long pspId;
    private String countryCode;
    private String currency;
    private String bankCode;
    private String bankName;
    private String bankShortName;
    private String pspBankCode;
    private Integer status;
    private String remark;
}
