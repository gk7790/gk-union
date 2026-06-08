package com.gk.openapi.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaymentMethodResponse {
    private String methodCode;
    private String methodName;
    private String countryCode;
    private String currency;
    private String direction;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
}
