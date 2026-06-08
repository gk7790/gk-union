package com.gk.openapi.service;

import com.gk.openapi.dto.PaymentMethodResponse;

import java.util.List;

public interface OpenPaymentMethodService {
    List<PaymentMethodResponse> list(String countryCode, String currency, String direction);
}
