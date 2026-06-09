package com.gk.openapi.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PayoutOrderResponse {
    private String payoutOrderNo;
    private String merchantOrderNo;
    private String status;
    private String statusReason;
    private String amount;
    private String currency;
    private String countryCode;
    private String methodCode;
    private String pspOrderNo;
}
