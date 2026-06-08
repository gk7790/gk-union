package com.gk.openapi.service;

import com.gk.openapi.dto.PayoutOrderCreateRequest;
import com.gk.openapi.dto.PayoutOrderResponse;

public interface OpenPayoutOrderService {
    PayoutOrderResponse create(PayoutOrderCreateRequest request, String idempotencyKey);

    PayoutOrderResponse getByPayoutOrderNo(String payoutOrderNo);

    PayoutOrderResponse getByMerchantOrderNo(String merchantOrderNo);
}
