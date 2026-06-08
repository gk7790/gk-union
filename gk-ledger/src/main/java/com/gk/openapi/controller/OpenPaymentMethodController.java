package com.gk.openapi.controller;

import com.gk.openapi.dto.ApiR;
import com.gk.openapi.dto.PaymentMethodResponse;
import com.gk.openapi.security.OpenApiRequestContextHolder;
import com.gk.openapi.service.OpenPaymentMethodService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/payment-methods")
@RequiredArgsConstructor
public class OpenPaymentMethodController {
    private final OpenPaymentMethodService openPaymentMethodService;

    @GetMapping
    public ApiR<List<PaymentMethodResponse>> list(
            @RequestParam(required = false) String countryCode,
            @RequestParam(required = false) String currency,
            @RequestParam(required = false) String direction
    ) {
        return ApiR.success(
                openPaymentMethodService.list(countryCode, currency, direction),
                OpenApiRequestContextHolder.getRequestId()
        );
    }
}
