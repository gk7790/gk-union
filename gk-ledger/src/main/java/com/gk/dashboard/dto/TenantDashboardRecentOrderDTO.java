package com.gk.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Schema(title = "租户首页最近订单")
public class TenantDashboardRecentOrderDTO {

    @Schema(title = "业务类型 PAY/PAYOUT")
    private String bizType;
    @Schema(title = "币种")
    private String currency;
    @Schema(title = "订单列表")
    private List<RecentOrder> items;

    @Data
    @Schema(title = "最近订单")
    public static class RecentOrder {
        @Schema(title = "订单ID")
        private Long orderId;
        @Schema(title = "平台订单号")
        private String orderNo;
        @Schema(title = "商户订单号")
        private String merchantOrderNo;
        @Schema(title = "商户ID")
        private Long merchantId;
        @Schema(title = "商户号")
        private String merchantNo;
        @Schema(title = "商户名称")
        private String merchantName;
        @Schema(title = "金额")
        private BigDecimal amount;
        @Schema(title = "币种")
        private String currency;
        @Schema(title = "状态")
        private String status;
        @Schema(title = "支付方式")
        private String methodCode;
        @Schema(title = "创建时间")
        private Instant createdAt;
        @Schema(title = "支付成功时间")
        private Instant paidAt;
        @Schema(title = "完成时间")
        private Instant completedAt;
        @Schema(title = "前端跳转路径")
        private String routePath;
    }
}
