package com.gk.dashboard.controller;

import com.gk.common.model.R;
import com.gk.dashboard.dto.TenantDashboardSummaryDTO;
import com.gk.dashboard.dto.TenantDashboardTodoDTO;
import com.gk.dashboard.dto.TenantDashboardTopMerchantDTO;
import com.gk.dashboard.dto.TenantDashboardTrendDTO;
import com.gk.dashboard.service.TenantDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "租户首页")
@RestController
@RequestMapping("/dashboard/tenant")
@RequiredArgsConstructor
public class TenantDashboardController {

    private final TenantDashboardService tenantDashboardService;

    @GetMapping("summary")
    @Operation(summary = "首页汇总")
    @PreAuthorize("hasAuthority('dashboard:tenant:view')")
    public R<TenantDashboardSummaryDTO> summary(
            @Parameter(description = "today/yesterday/last7d/last30d")
            @RequestParam(defaultValue = "today") String range,
            @RequestParam(required = false) String currency,
            @RequestParam(defaultValue = "false") boolean compare) {
        return R.ok(tenantDashboardService.summary(range, currency, compare));
    }

    @GetMapping("trend")
    @Operation(summary = "首页趋势")
    @PreAuthorize("hasAuthority('dashboard:tenant:view')")
    public R<TenantDashboardTrendDTO> trend(
            @Parameter(description = "today/yesterday/last7d/last30d，默认 last7d")
            @RequestParam(defaultValue = "last7d") String range,
            @RequestParam(required = false) String currency) {
        return R.ok(tenantDashboardService.trend(range, currency));
    }

    @GetMapping("todos")
    @Operation(summary = "待办明细")
    @PreAuthorize("hasAuthority('dashboard:tenant:view')")
    public R<TenantDashboardTodoDTO> todos(
            @Parameter(description = "MANUAL_REVIEW/NOTIFY_FAILED/SETTLE_DUE/PROCESSING_PAY/PROCESSING_PAYOUT，也支持 summary.todos 的 camelCase")
            @RequestParam String type,
            @RequestParam(required = false) String currency,
            @Parameter(description = "默认10，最大50")
            @RequestParam(defaultValue = "10") int limit) {
        return R.ok(tenantDashboardService.todos(type, currency, limit));
    }

    @GetMapping("top-merchants")
    @Operation(summary = "Top商户排行")
    @PreAuthorize("hasAuthority('dashboard:tenant:view')")
    public R<TenantDashboardTopMerchantDTO> topMerchants(
            @Parameter(description = "today/yesterday/last7d/last30d，默认 today")
            @RequestParam(defaultValue = "today") String range,
            @RequestParam(required = false) String currency,
            @Parameter(description = "默认5，最大20")
            @RequestParam(defaultValue = "5") int limit) {
        return R.ok(tenantDashboardService.topMerchants(range, currency, limit));
    }
}
