package com.gk.openapi.service.impl;

import com.gk.common.model.DynMap;
import com.gk.openapi.dto.PaymentMethodResponse;
import com.gk.openapi.service.OpenPaymentMethodService;
import com.gk.payment.dto.PaymentMethodDTO;
import com.gk.payment.service.PaymentMethodService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

/**
 * 商户 OpenAPI 支付方式查询实现�? *
 * <p>这里读取 payment_method 标准支付方式字典，用于告诉商户系统支持哪�?method_code�? * 真正下单时仍由商户费率、支付计划、PSP 路由、PSP Method 和银行映射决定最终可用通道�?/p>
 */
@Service
@RequiredArgsConstructor
public class OpenPaymentMethodServiceImpl implements OpenPaymentMethodService {
    private final PaymentMethodService paymentMethodService;

    /**
     * 查询商户侧可展示的标准支付方式�?     *
     * <p>countryCode、currency、direction 会传�?PaymentMethodService�?     * 支持精确配置，也支持 payment_method �?country/currency/direction 为空�?BOTH 的通用配置�?/p>
     */
    @Override
    public List<PaymentMethodResponse> list(String countryCode, String currency, String direction) {
        DynMap params = new DynMap();
        params.put("countryCode", countryCode);
        params.put("currency", currency);
        params.put("direction", direction);
        return paymentMethodService.getDict(params).stream()
                .map(this::toResponse)
                .sorted(Comparator.comparing(PaymentMethodResponse::getMethodCode))
                .toList();
    }

    /**
     * 转换为商�?OpenAPI 响应模型，只暴露商户需要识别的标准方式信息�?     */
    private PaymentMethodResponse toResponse(PaymentMethodDTO entity) {
        PaymentMethodResponse response = new PaymentMethodResponse();
        response.setMethodCode(entity.getMethodCode());
        response.setMethodName(entity.getMethodName());
        response.setCountryCode(entity.getCountryCode());
        response.setCurrency(entity.getCurrency());
        response.setDirection(entity.getDirection());
        return response;
    }
}
