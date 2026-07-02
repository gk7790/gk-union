package com.gk.payment.merchantview;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.gk.openapi.dto.OpenApiModel;
import lombok.Data;

@Data
@OpenApiModel
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class MerchantOrderView {
    private String systemOrderId;
    private String merchantOrderId;
    private String status;
    private String statusReason;
    private String amount;
    private String currency;
    private String countryCode;
    private String methodCode;
    private String feeAmount;
}
