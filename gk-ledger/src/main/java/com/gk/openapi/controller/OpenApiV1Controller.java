package com.gk.openapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gk.openapi.dto.*;
import com.gk.openapi.error.ApiErrorCode;
import com.gk.openapi.error.ApiException;
import com.gk.openapi.security.OpenApiAuthFilter;
import com.gk.openapi.service.OpenBalanceService;
import com.gk.openapi.service.OpenPayOrderService;
import com.gk.openapi.service.OpenPaymentMethodService;
import com.gk.openapi.service.OpenPayoutOrderService;
import com.gk.openapi.tools.ApiR;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class OpenApiV1Controller {
    private final OpenBalanceService openBalanceService;
    private final OpenPaymentMethodService openPaymentMethodService;
    private final OpenPayOrderService openPayOrderService;
    private final OpenPayoutOrderService openPayoutOrderService;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    /**
     * 查询余额
     * @return 余额
     */
    @PostMapping("balance")
    public ApiR<List<BalanceResponse>> balance(HttpServletRequest request) {
        BalanceQueryRequest body = bindSignParams(request, BalanceQueryRequest.class, false);
        return ApiR.success(openBalanceService.list(body.getCurrency()));
    }

    @PostMapping( "pay/create")
    public ApiR<PayOrderResponse> createPay(HttpServletRequest request) {
        PayOrderCreateRequest body = bindSignParams(request, PayOrderCreateRequest.class, true);
        PayOrderResponse orderResp = openPayOrderService.create(body);
        return ApiR.success(orderResp);
    }

    @PostMapping( "pay/query")
    public ApiR<PayOrderResponse> queryPay(HttpServletRequest request) {
        PayOrderQueryRequest body = bindSignParams(request, PayOrderQueryRequest.class, false);
        PayOrderResponse response;
        if (StringUtils.isNotBlank(body.getPayOrderNo())) {
            response = openPayOrderService.getByPayOrderNo(body.getPayOrderNo());
        } else if (StringUtils.isNotBlank(body.getMerchantOrderNo())) {
            response = openPayOrderService.getByMerchantOrderNo(body.getMerchantOrderNo());
        } else {
            throw new ApiException(ApiErrorCode.INVALID_REQUEST, "system_order_id or merchant_order_id is required");
        }
        return ApiR.success(response);
    }

    @PostMapping( "payout/create")
    public ApiR<PayoutOrderResponse> createPayout(HttpServletRequest request) {
        PayoutOrderCreateRequest body = bindSignParams(request, PayoutOrderCreateRequest.class, true);
        return ApiR.success(openPayoutOrderService.create(body));
    }

    @PostMapping("payout/query")
    public ApiR<PayoutOrderResponse> queryPayout(HttpServletRequest request) {
        PayoutOrderQueryRequest body = bindSignParams(request, PayoutOrderQueryRequest.class, false);
        PayoutOrderResponse response;
        if (StringUtils.isNotBlank(body.getPayoutOrderNo())) {
            response = openPayoutOrderService.getByPayoutOrderNo(body.getPayoutOrderNo());
        } else if (StringUtils.isNotBlank(body.getMerchantOrderNo())) {
            response = openPayoutOrderService.getByMerchantOrderNo(body.getMerchantOrderNo());
        } else {
            throw new ApiException(ApiErrorCode.INVALID_REQUEST, "system_order_id or merchant_order_id is required");
        }
        return ApiR.success(response);
    }

    @PostMapping("methods")
    public ApiR<List<PaymentMethodResponse>> methods(HttpServletRequest request) {
        PaymentMethodQueryRequest body = bindSignParams(request, PaymentMethodQueryRequest.class, false);
        return ApiR.success(openPaymentMethodService.list(body.getCountryCode(), body.getCurrency(), body.getDirection()));
    }

    @SuppressWarnings("unchecked")
    private <T> T bindSignParams(HttpServletRequest request, Class<T> requestType, boolean validate) {
        Object value = request.getAttribute(OpenApiAuthFilter.ATTR_SIGN_PARAMS);
        if (!(value instanceof Map<?, ?> params)) {
            throw new ApiException(ApiErrorCode.INVALID_REQUEST, "request parameters is empty");
        }
        T body = objectMapper.convertValue((Map<String, Object>) params, requestType);
        if (validate) {
            Set<ConstraintViolation<T>> violations = validator.validate(body);
            if (!violations.isEmpty()) {
                throw new ApiException(ApiErrorCode.INVALID_REQUEST, violations.iterator().next().getMessage());
            }
        }
        return body;
    }
}
