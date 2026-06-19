package com.gk.dashboard.controller;

import com.gk.common.model.R;
import com.gk.dashboard.dto.TenantDashboardSummaryDTO;
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
}
