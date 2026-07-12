package com.gk.payment.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PayinConfigPrecheckRequest {
    private Long tenantId;
    private Long merchantId;
    private Long merchantAppId;
    private String countryCode;
    private String currency;
    private String methodCode;
    private BigDecimal amount;
}
