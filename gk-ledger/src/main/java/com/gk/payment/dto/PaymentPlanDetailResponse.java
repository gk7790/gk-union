package com.gk.payment.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
public class PaymentPlanDetailResponse {
    private Catalog catalog;
    private List<Bucket> buckets = new ArrayList<>();

    @Data
    public static class Catalog {
        private Long id;
        private Long tenantId;
        private Long merchantId;
        private Long merchantAppId;
        private String direction;
        private String countryCode;
        private String currency;
        private String methodCode;
        private Long version;
        private String status;
        private Integer bucketCount;
        private Integer routeOptionCount;
        private String configHash;
        private Instant compiledAt;
        private Instant activatedAt;
        private String remark;
        private Long createdBy;
        private Instant createdAt;
        private Long updatedBy;
        private Instant updatedAt;
    }

    @Data
    public static class Bucket {
        private Long id;
        private Long tenantId;
        private Long catalogId;
        private BigDecimal bucketStartAmount;
        private BigDecimal bucketEndAmount;
        private Long merchantFeeRuleId;
        private String merchantFeeSnapshotJson;
        private Integer sort;
        private List<RouteOption> routeOptions = new ArrayList<>();
    }

    @Data
    public static class RouteOption {
        private Long id;
        private Long tenantId;
        private Long catalogId;
        private Long bucketId;
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
        private Integer sort;
    }
}
