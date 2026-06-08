package com.gk.openapi.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PayOrderResponse {
    private String payOrderNo;
    private String merchantOrderNo;
    private String status;
    private String statusReason;
    private BigDecimal amount;
    private BigDecimal paidAmount;
    private String currency;
    private String countryCode;
    private String methodCode;
    private String payUrl;
    private String pspOrderNo;
}
