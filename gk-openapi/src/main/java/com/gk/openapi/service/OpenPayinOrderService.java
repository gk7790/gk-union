package com.gk.openapi.service;

import com.gk.openapi.dto.PayinOrderCreateRequest;
import com.gk.openapi.dto.PayinOrderResponse;

public interface OpenPayinOrderService {
    PayinOrderResponse create(PayinOrderCreateRequest request);

    PayinOrderResponse getByPayinOrderNo(String payinOrderNo);

    PayinOrderResponse getByMerchantOrderNo(String merchantOrderNo);
}
