package com.gk.payment.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class PaymentRouteChannelOptionsResponse {
    private RouteGroupOption routeGroup;
    private List<PspProviderOption> pspProviders = new ArrayList<>();
    private List<PspMethodOption> pspMethods = new ArrayList<>();
    private List<PspAccountOption> pspAccounts = new ArrayList<>();
    private List<PspFeeRuleOption> pspFeeRules = new ArrayList<>();

    @Data
    public static class RouteGroupOption {
        private Long id;
        private Long tenantId;
        private String groupCode;
        private String groupName;
        private String direction;
        private String countryCode;
        private String currency;
        private String methodCode;
        private String strategy;
        private Integer status;
        private String label;
        private Long value;
    }

    @Data
    public static class PspProviderOption {
        private String label;
        private Long value;
        private Long pspId;
        private String pspCode;
        private String pspName;
        private String countryCode;
        private String apiVersion;
        private Integer supportPayin;
        private Integer supportPayout;
        private Integer status;
    }

    @Data
    public static class PspMethodOption {
        private String label;
        private Long value;
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
    }

    @Data
    public static class PspAccountOption {
        private String label;
        private Long value;
        private Long pspAccountId;
        private Long pspId;
        private String pspAccountNo;
        private String pspAccountName;
        private String secretType;
        private Integer status;
    }

    @Data
    public static class PspFeeRuleOption {
        private String label;
        private Long value;
        private Long pspFeeRuleId;
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
        private BigDecimal feeRate;
        private BigDecimal feeFixed;
        private BigDecimal minFee;
        private BigDecimal maxFee;
        private Integer priority;
        private Integer status;
    }
}
