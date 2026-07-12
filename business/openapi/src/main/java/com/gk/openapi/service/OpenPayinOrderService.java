package com.gk.openapi.service;

import com.gk.openapi.dto.PayinOrderCreateRequest;
import com.gk.payment.merchantview.PayinOrderView;

public interface OpenPayinOrderService {
    PayinOrderView create(PayinOrderCreateRequest request);

    PayinOrderView getByPayinOrderNo(String payinOrderNo);

    PayinOrderView getByMerchantOrderNo(String merchantOrderNo);
}
