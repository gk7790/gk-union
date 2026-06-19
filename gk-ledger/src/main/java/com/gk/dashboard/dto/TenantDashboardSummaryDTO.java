package com.gk.dashboard.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Schema(title = "租户首页汇总")
public class TenantDashboardSummaryDTO {

    @Schema(title = "元信息")
    private Meta meta;
    @Schema(title = "代收指标")
    private OrderMetric payIn;
    @Schema(title = "代付指标")
    private OrderMetric payOut;
    @Schema(title = "代收环比")
    private CompareMetric payInCompare;
    @Schema(title = "代付环比")
    private CompareMetric payOutCompare;
    @Schema(title = "活跃商户数")
    private Long activeMerchantCount;
    @Schema(title = "商户数量")
    private MerchantCount merchantCount;
    @Schema(title = "资金摘要")
    private Balance balance;
    @Schema(title = "待办计数")
    private Todo todos;

    @Data
    @Schema(title = "元信息")
    public static class Meta {
        private Long tenantId;
        private String tenantName;
        private String timezone;
        private String currency;
        private String range;
        private Instant rangeStart;
        private Instant rangeEnd;
        private Instant compareRangeStart;
        private Instant compareRangeEnd;
    }

    @Data
    @Schema(title = "订单指标")
    public static class OrderMetric {
        private Long successCount;
        private BigDecimal successAmount;
        private BigDecimal successRate;
        private BigDecimal feeAmount;
        private Long failedCount;
        private Long processingCount;
        @JsonIgnore
        private Long closedCount;
    }

    @Data
    @Schema(title = "环比")
    public static class CompareMetric {
        private Long successCountDelta;
        private BigDecimal successAmountDelta;
        private BigDecimal successRateDelta;
    }

    @Data
    @Schema(title = "商户数量")
    public static class MerchantCount {
        private Long total;
        private Long enabled;
    }

    @Data
    @Schema(title = "资金摘要")
    public static class Balance {
        private BigDecimal available;
        private BigDecimal frozen;
        private BigDecimal pendingSettle;
        private BigDecimal total;
        private Long merchantCount;
    }

    @Data
    @Schema(title = "待办计数")
    public static class Todo {
        private Long manualReview;
        private Long notifyFailed;
        private Long settleDue;
        private Long processingPay;
        private Long processingPayout;
    }
}
