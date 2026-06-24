package com.gk.payment.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class PaymentPlanBatchPreviewResponse {
    private Long tenantId;
    private Long routeGroupId;
    private String groupCode;
    private String groupName;
    private Integer total = 0;
    private Integer validCount = 0;
    private Integer invalidCount = 0;
    private Integer skippedCount = 0;
    private List<Item> items = new ArrayList<>();

    @Data
    public static class Item {
        private Long routeRuleId;
        private String routeRuleName;
        private Long merchantId;
        private Long merchantAppId;
        private String direction;
        private String countryCode;
        private String currency;
        private String methodCode;
        private BigDecimal minAmount;
        private BigDecimal maxAmount;
        private Boolean valid;
        private Boolean skipped = false;
        private String skipReason;
        private Integer bucketCount = 0;
        private Integer routeOptionCount = 0;
        private Integer warningCount = 0;
        private Integer errorCount = 0;
        private List<PaymentPlanPreviewResponse.Message> warnings = new ArrayList<>();
        private List<PaymentPlanPreviewResponse.Message> errors = new ArrayList<>();
        private PaymentPlanPreviewResponse preview;
    }
}
