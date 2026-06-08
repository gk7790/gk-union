package com.gk.openapi.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
public class PayoutOrderCreateRequest {
    private String merchantOrderNo;
    private BigDecimal amount;
    private String currency;
    private String countryCode;
    private String methodCode;
    private String purpose;
    private String notifyUrl;
    private Beneficiary beneficiary;
    private Map<String, Object> extra;

    @Data
    public static class Beneficiary {
        private String name;
        private String accountNo;
        private String bankCode;
        private String walletType;
        private String phone;
        private String email;
    }
}
