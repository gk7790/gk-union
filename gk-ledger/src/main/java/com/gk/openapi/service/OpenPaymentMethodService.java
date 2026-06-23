package com.gk.openapi.service;

import com.gk.openapi.dto.PaymentMethodResponse;

import java.util.List;

/**
 * 商户 OpenAPI 支付方式查询服务。
 *
 * <p>该服务只用于向商户展示系统标准支付方式，不参与下单、费率计算或 PSP 路由决策。</p>
 */
public interface OpenPaymentMethodService {
    /**
     * 按国家/地区、币种和交易方向查询商户可见的标准支付方式。
     */
    List<PaymentMethodResponse> list(String countryCode, String currency, String direction);
}
