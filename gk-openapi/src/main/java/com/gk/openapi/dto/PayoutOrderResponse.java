package com.gk.openapi.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@OpenApiModel
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PayoutOrderResponse {
    private String systemOrderId;
    private String merchantOrderId;
    private String status;
    private String statusReason;
    private String amount;
    private String currency;
    private String countryCode;
    private String methodCode;
}
