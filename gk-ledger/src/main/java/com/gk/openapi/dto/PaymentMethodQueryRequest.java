package com.gk.openapi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@OpenApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class PaymentMethodQueryRequest {
    private String countryCode;
    private String currency;
    private String direction;
}
