package com.gk.dashboard.controller;

import com.gk.common.model.R;
import com.gk.dashboard.dto.TenantDashboardRecentOrderDTO;
import com.gk.dashboard.dto.TenantDashboardSummaryDTO;
import com.gk.dashboard.dto.TenantDashboardTodoDTO;
import com.gk.dashboard.dto.TenantDashboardTrendDTO;
import com.gk.dashboard.service.MerchantDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "商户首页")
@RestController
@RequestMapping("/dashboard/merchant")
@RequiredArgsConstructor
public class MerchantDashboardController {

    private final MerchantDashboardService merchantDashboardService;

    @GetMapping("summary")
    @Operation(summary = "首页汇")
    @PreAuthorize("hasAuthority('dashboard:merchant:view')")
    public R<TenantDashboardSummaryDTO> summary(
            @Parameter(description = "today/yesterday/last7d/last30d")
            @RequestParam(defaultValue = "today") String range,
            @RequestParam(required = false) String currency,
            @RequestParam(defaultValue = "false") boolean compare) {
        return R.ok(merchantDashboardService.summary(range, currency, compare));
    }

    @GetMapping("trend")
    @Operation(summary = "首页趋势")
    @PreAuthorize("hasAuthority('dashboard:merchant:view')")
    public R<TenantDashboardTrendDTO> trend(
            @Parameter(description = "today/yesterday/last7d/last30d，默last7d")
            @RequestParam(defaultValue = "last7d") String range,
            @RequestParam(required = false) String currency) {
        return R.ok(merchantDashboardService.trend(range, currency));
    }

    @GetMapping("todos")
    @Operation(summary = "待办明细")
    @PreAuthorize("hasAuthority('dashboard:merchant:view')")
    public R<TenantDashboardTodoDTO> todos(
            @Parameter(description = "MANUAL_REVIEW/NOTIFY_FAILED/PROCESSING_PAYIN/PROCESSING_PAYOUT")
            @RequestParam String type,
            @RequestParam(required = false) String currency,
            @Parameter(description = "默认10，最0")
            @RequestParam(defaultValue = "10") int limit) {
        return R.ok(merchantDashboardService.todos(type, currency, limit));
    }

    @GetMapping("recent-orders")
    @Operation(summary = "最近订")
    @PreAuthorize("hasAuthority('dashboard:merchant:view')")
    public R<TenantDashboardRecentOrderDTO> recentOrders(
            @Parameter(description = "PAYIN/PAYOUT")
            @RequestParam String bizType,
            @RequestParam(required = false) String currency,
            @Parameter(description = "默认5，最0")
            @RequestParam(defaultValue = "5") int limit) {
        return R.ok(merchantDashboardService.recentOrders(bizType, currency, limit));
    }
}
