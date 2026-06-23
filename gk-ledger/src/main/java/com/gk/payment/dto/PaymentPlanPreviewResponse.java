package com.gk.payment.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalTime;
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
        private String amountRangeText;
        private Long merchantFeeRuleId;
        private MerchantFeeRule merchantFeeRule;
        private List<RouteOption> routeOptions = new ArrayList<>();
    }

    @Data
    public static class RouteOption {
        private Long routeRuleId;
        private Long routeGroupId;
        private Long routeChannelId;
        private Long pspId;
        private String pspCode;
        private Long pspMethodId;
        private String pspMethodCode;
        private Long pspAccountId;
        private String pspAccountNo;
        private Long pspFeeRuleId;
        private String pspFeeSnapshotJson;
        private String routeRuleSnapshotJson;
        private String routeGroupSnapshotJson;
        private String routeChannelSnapshotJson;
        private String pspProviderSnapshotJson;
        private String pspMethodSnapshotJson;
        private String pspAccountSnapshotJson;
        private Integer priority;
        private Integer weight;
        private Integer fallbackOrder;
        private String status;
        private Route route;
        private RouteGroup routeGroup;
        private RouteChannel routeChannel;
        private Psp psp;
        private PspMethod pspMethod;
        private PspAccount pspAccount;
        private PspFeeRule pspFeeRule;
    }

    @Data
    public static class MerchantFeeRule {
        private Long id;
        private String ruleName;
        private String direction;
        private String countryCode;
        private String currency;
        private String methodCode;
        private BigDecimal minAmount;
        private BigDecimal maxAmount;
        private String feeMode;
        private String feeModeName;
        private BigDecimal feeRate;
        private BigDecimal feeFixed;
        private BigDecimal minFee;
        private BigDecimal maxFee;
        private String feeBearer;
        private String feeBearerName;
        private String settleMode;
        private Integer priority;
        private Integer status;
        private String remark;
    }

    @Data
    public static class Route {
        private Long routeRuleId;
        private String routeName;
        private String routeMode;
        private String countryCode;
        private String currency;
        private String methodCode;
        private String direction;
        private BigDecimal minAmount;
        private BigDecimal maxAmount;
        private LocalTime startTime;
        private LocalTime endTime;
        private Integer priority;
        private Integer weight;
        private Integer fallbackOrder;
        private Integer status;
        private String remark;
    }

    @Data
    public static class RouteGroup {
        private Long routeGroupId;
        private String groupCode;
        private String groupName;
        private String direction;
        private String countryCode;
        private String currency;
        private String methodCode;
        private String strategy;
        private Integer status;
        private String remark;
    }

    @Data
    public static class RouteChannel {
        private Long routeChannelId;
        private Long routeGroupId;
        private Long pspId;
        private Long pspMethodId;
        private Long pspAccountId;
        private Integer priority;
        private Integer weight;
        private Integer fallbackOrder;
        private BigDecimal minAmount;
        private BigDecimal maxAmount;
        private Integer status;
        private String remark;
    }

    @Data
    public static class Psp {
        private Long pspId;
        private String pspCode;
        private String pspName;
        private String countryCode;
        private String apiVersion;
        private Integer supportPayin;
        private Integer supportPayout;
        private Integer status;
        private String remark;
    }

    @Data
    public static class PspMethod {
        private Long pspMethodId;
        private Long pspId;
        private String pspCode;
        private String methodCode;
        private String pspMethodCode;
        private String methodName;
        private String countryCode;
        private String currency;
        private String direction;
        private BigDecimal minAmount;
        private BigDecimal maxAmount;
        private BigDecimal dailyLimit;
        private Integer status;
        private String remark;
    }

    @Data
    public static class PspAccount {
        private Long pspAccountId;
        private Long pspId;
        private String pspAccountNo;
        private String pspAccountName;
        private String secretType;
        private Integer status;
        private String remark;
    }

    @Data
    public static class PspFeeRule {
        private Long id;
        private Long pspId;
        private Long pspAccountId;
        private Long pspMethodId;
        private String pspMethodCode;
        private String ruleName;
        private String direction;
        private String countryCode;
        private String currency;
        private String methodCode;
        private BigDecimal minAmount;
        private BigDecimal maxAmount;
        private String feeMode;
        private String feeModeName;
        private BigDecimal feeRate;
        private BigDecimal feeFixed;
        private BigDecimal minFee;
        private BigDecimal maxFee;
        private Integer priority;
        private Integer status;
        private String remark;
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
