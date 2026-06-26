package com.gk.openapi.service;

import com.gk.openapi.dto.PayOrderCreateRequest;
import com.gk.openapi.dto.PayOrderResponse;

public interface OpenPayOrderService {
    PayOrderResponse create(PayOrderCreateRequest request);

    PayOrderResponse getByPayOrderNo(String payOrderNo);

    PayOrderResponse getByMerchantOrderNo(String merchantOrderNo);
}
