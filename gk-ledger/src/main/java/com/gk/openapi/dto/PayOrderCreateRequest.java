package com.gk.openapi.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
public class PayOrderCreateRequest {
    private String merchantOrderNo;
    private BigDecimal amount;
    private String currency;
    private String countryCode;
    private String methodCode;
    private String subject;
    private String description;
    private String notifyUrl;
    private String returnUrl;
    private Map<String, Object> payer;
    private Map<String, Object> extra;
}
