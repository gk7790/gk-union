package com.gk.openapi.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PayOrderResponse {
    private String payOrderNo;
    private String merchantOrderNo;
    private String status;
    private String statusReason;
    private String amount;
    private String currency;
    private String countryCode;
    private String methodCode;
    private String payUrl;
    private String pspOrderNo;
}
