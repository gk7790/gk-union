package com.gk.openapi.controller;

import com.gk.openapi.dto.ApiR;
import com.gk.openapi.dto.BalanceResponse;
import com.gk.openapi.security.OpenApiRequestContextHolder;
import com.gk.openapi.service.OpenBalanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/balances")
@RequiredArgsConstructor
public class OpenBalanceController {
    private final OpenBalanceService openBalanceService;

    @GetMapping
    public ApiR<List<BalanceResponse>> list(@RequestParam(required = false) String currency) {
        return ApiR.success(
                openBalanceService.list(currency),
                OpenApiRequestContextHolder.getRequestId()
        );
    }
}
