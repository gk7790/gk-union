package com.gk.openapi.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PayoutOrderResponse {
    private String payoutOrderNo;
    private String merchantOrderNo;
    private String status;
    private String statusReason;
    private BigDecimal amount;
    private String currency;
    private String countryCode;
    private String methodCode;
    private String pspOrderNo;
}
