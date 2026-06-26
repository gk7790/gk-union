package com.gk.psp.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PspMethodDictDTO {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long value;
    private String label;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long pspMethodId;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long pspId;
    private String pspCode;
    private String methodCode;
    private String pspMethodCode;
    private String methodName;
    private String countryCode;
    private String currency;
    private String direction;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
}
