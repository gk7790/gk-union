package com.gk.openapi.service;

import com.gk.openapi.dto.PayoutOrderCreateRequest;
import com.gk.payment.merchantview.PayoutOrderView;

public interface OpenPayoutOrderService {
    PayoutOrderView create(PayoutOrderCreateRequest request);

    PayoutOrderView getByPayoutOrderNo(String payoutOrderNo);

    PayoutOrderView getByMerchantOrderNo(String merchantOrderNo);
}
