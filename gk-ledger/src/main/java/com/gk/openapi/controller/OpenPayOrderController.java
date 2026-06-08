package com.gk.openapi.controller;

import com.gk.openapi.dto.ApiR;
import com.gk.openapi.dto.PayOrderCreateRequest;
import com.gk.openapi.dto.PayOrderResponse;
import com.gk.openapi.security.OpenApiRequestContextHolder;
import com.gk.openapi.service.OpenPayOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/pay/orders")
@RequiredArgsConstructor
public class OpenPayOrderController {
    private final OpenPayOrderService openPayOrderService;

    @PostMapping
    public ApiR<PayOrderResponse> create(
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody PayOrderCreateRequest request
    ) {
        return ApiR.success(
                openPayOrderService.create(request, idempotencyKey),
                OpenApiRequestContextHolder.getRequestId()
        );
    }

    @GetMapping("{payOrderNo}")
    public ApiR<PayOrderResponse> getByPayOrderNo(@PathVariable String payOrderNo) {
        return ApiR.success(
                openPayOrderService.getByPayOrderNo(payOrderNo),
                OpenApiRequestContextHolder.getRequestId()
        );
    }

    @GetMapping("by-merchant-no/{merchantOrderNo}")
    public ApiR<PayOrderResponse> getByMerchantOrderNo(@PathVariable String merchantOrderNo) {
        return ApiR.success(
                openPayOrderService.getByMerchantOrderNo(merchantOrderNo),
                OpenApiRequestContextHolder.getRequestId()
        );
    }
}
