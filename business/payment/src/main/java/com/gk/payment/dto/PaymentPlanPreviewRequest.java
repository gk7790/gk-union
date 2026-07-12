package com.gk.payment.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.gk.payment.dto.PaymentJsonModel;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@PaymentJsonModel
public class PaymentPlanPreviewRequest {
    @JsonAlias("tenantId")
    private Long tenantId;
    @JsonAlias("merchantId")
    private Long merchantId;
    @JsonAlias("merchantAppId")
    private Long merchantAppId;
    @JsonAlias("merchantFeeRuleId")
    private Long merchantFeeRuleId;
    private String direction;
    private String currency;
    @JsonAlias("countryCode")
    private String countryCode;
    @JsonAlias("methodCode")
    private String methodCode;
    @JsonAlias("minAmount")
    private BigDecimal minAmount;
    @JsonAlias("maxAmount")
    private BigDecimal maxAmount;
    @JsonAlias("pspFeeRequired")
    private Boolean pspFeeRequired;
    @JsonAlias("testCases")
    private List<TestCase> testCases;

    @Data
    @PaymentJsonModel
    public static class TestCase {
        @JsonAlias("merchantOrderId")
        private String merchantOrderId;
        private BigDecimal amount;
        @JsonAlias("notifyUrl")
        private String notifyUrl;
        private Payee payee;
    }

    @Data
    @PaymentJsonModel
    public static class Payee {
        private String name;
        @JsonAlias("accountNo")
        private String accountNo;
        @JsonAlias("bankCode")
        private String bankCode;
        @JsonAlias("walletType")
        private String walletType;
        private String phone;
        private String email;
    }
}
