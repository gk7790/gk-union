package com.gk.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Schema(title = "租户首页待办")
public class TenantDashboardTodoDTO {

    @Schema(title = "待办类型")
    private String type;
    @Schema(title = "币种")
    private String currency;
    @Schema(title = "待办列表")
    private List<TodoItem> items;

    @Data
    @Schema(title = "待办�?)
    public static class TodoItem {
        @Schema(title = "业务类型 PAY/PAYOUT")
        private String bizType;
        @Schema(title = "订单ID")
        private Long orderId;
        @Schema(title = "平台订单�?)
        private String orderNo;
        @Schema(title = "商户ID")
        private Long merchantId;
        @Schema(title = "商户�?)
        private String merchantNo;
        @Schema(title = "商户名称")
        private String merchantName;
        @Schema(title = "金额")
        private BigDecimal amount;
        @Schema(title = "币种")
        private String currency;
        @Schema(title = "状�?)
        private String status;
        @Schema(title = "事件时间")
        private Instant eventAt;
        @Schema(title = "原因")
        private String reason;
        @Schema(title = "前端跳转路径")
        private String routePath;
    }
}
