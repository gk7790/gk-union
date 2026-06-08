package com.gk.openapi.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PaymentMethodQueryRequest {
    @JsonAlias("country_code")
    private String countryCode;
    private String currency;
    private String direction;
}
