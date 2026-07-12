package com.gk.payment.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class PaymentRouteGroupCheckResponse {
    private Boolean valid = true;
    private Long groupId;
    private String groupCode;
    private String groupName;
    private String direction;
    private String countryCode;
    private String currency;
    private String methodCode;
    private List<RouteRuleCheck> routeRules = new ArrayList<>();
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
    public static class RouteRuleCheck {
        private Boolean valid = true;
        private Long routeRuleId;
        private String ruleName;
        private Long merchantId;
        private Long merchantAppId;
        private BigDecimal minAmount;
        private BigDecimal maxAmount;
        private List<AmountRange> coveredRanges = new ArrayList<>();
        private List<AmountRange> gaps = new ArrayList<>();
        private List<ChannelCoverage> channels = new ArrayList<>();
        private List<Message> warnings = new ArrayList<>();
        private List<Message> errors = new ArrayList<>();

        public void addWarning(String code, String message) {
            warnings.add(new Message(code, message));
        }

        public void addError(String code, String message) {
            valid = false;
            errors.add(new Message(code, message));
        }
    }

    @Data
    public static class ChannelCoverage {
        private Boolean available = true;
        private Long routeChannelId;
        private Long pspId;
        private Long pspMethodId;
        private String pspCode;
        private String pspMethodCode;
        private Long pspAccountId;
        private Long pspFeeRuleId;
        private BigDecimal minAmount;
        private BigDecimal maxAmount;
        private String message;
    }

    public record AmountRange(BigDecimal minAmount, BigDecimal maxAmount) {
    }
}
