package com.gk.payment.plan;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class PaymentPlanCompileRequest {
    private Long tenantId;
    private Long merchantId;
    private Long merchantAppId;
    private String direction;
    private String countryCode;
    private String currency;
    private String methodCode;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    private Boolean pspFeeRequired;
    private String remark;
    private List<TestCase> testCases = new ArrayList<>();

    @Data
    public static class TestCase {
        private String merchantOrderId;
        private BigDecimal amount;
        private String bankCode;
    }
}
