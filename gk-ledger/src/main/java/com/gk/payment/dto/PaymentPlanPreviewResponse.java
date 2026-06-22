package com.gk.payment.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class PaymentPlanPreviewResponse {
    private Boolean valid = true;
    private String direction;
    private String currency;
    private String countryCode;
    private String methodCode;
    private Integer bucketCount = 0;
    private Integer routeOptionCount = 0;
    private List<Bucket> buckets = new ArrayList<>();
    private List<TestResult> testResults = new ArrayList<>();
    private List<Message> warnings = new ArrayList<>();
    private List<Message> errors = new ArrayList<>();

    public void addWarning(String code, String message) {
        warnings.add(new Message(code, message));
    }

    public void addError(String code, String message) {
        valid = false;
        errors.add(new Message(code, message));
    }

    public record Message(String code, String message) {
    }

    @Data
    public static class Bucket {
        private BigDecimal startAmount;
        private BigDecimal endAmount;
        private Long merchantFeeRuleId;
        private List<RouteOption> routeOptions = new ArrayList<>();
    }

    @Data
    public static class RouteOption {
        private Long routeRuleId;
        private Long pspId;
        private String pspCode;
        private Long pspMethodId;
        private String pspMethodCode;
        private Long pspAccountId;
        private String pspAccountNo;
        private Long pspFeeRuleId;
        private Integer priority;
        private Integer weight;
        private Integer fallbackOrder;
        private String status;
    }

    @Data
    public static class TestResult {
        private String merchantOrderId;
        private BigDecimal amount;
        private Boolean matched;
        private String message;
        private Long merchantFeeRuleId;
        private BigDecimal merchantFeeAmount;
        private BigDecimal settleAmount;
        private BigDecimal totalDebitAmount;
        private Long pspId;
        private String pspCode;
        private Long pspAccountId;
        private Long pspFeeRuleId;
        private BigDecimal pspFeeAmount;
        private String bankCode;
        private String pspBankCode;
    }
}
