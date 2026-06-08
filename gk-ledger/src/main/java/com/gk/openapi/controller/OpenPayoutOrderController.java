package com.gk.openapi.controller;

import com.gk.openapi.dto.ApiR;
import com.gk.openapi.dto.PayoutOrderCreateRequest;
import com.gk.openapi.dto.PayoutOrderResponse;
import com.gk.openapi.security.OpenApiRequestContextHolder;
import com.gk.openapi.service.OpenPayoutOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payout/orders")
@RequiredArgsConstructor
public class OpenPayoutOrderController {
    private final OpenPayoutOrderService openPayoutOrderService;

    @PostMapping
    public ApiR<PayoutOrderResponse> create(
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody PayoutOrderCreateRequest request
    ) {
        return ApiR.success(
                openPayoutOrderService.create(request, idempotencyKey),
                OpenApiRequestContextHolder.getRequestId()
        );
    }

    @GetMapping("{payoutOrderNo}")
    public ApiR<PayoutOrderResponse> getByPayoutOrderNo(@PathVariable String payoutOrderNo) {
        return ApiR.success(
                openPayoutOrderService.getByPayoutOrderNo(payoutOrderNo),
                OpenApiRequestContextHolder.getRequestId()
        );
    }

    @GetMapping("by-merchant-no/{merchantOrderNo}")
    public ApiR<PayoutOrderResponse> getByMerchantOrderNo(@PathVariable String merchantOrderNo) {
        return ApiR.success(
                openPayoutOrderService.getByMerchantOrderNo(merchantOrderNo),
                OpenApiRequestContextHolder.getRequestId()
        );
    }
}
