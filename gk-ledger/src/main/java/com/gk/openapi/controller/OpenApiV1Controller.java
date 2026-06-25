package com.gk.openapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gk.openapi.dto.*;
import com.gk.openapi.error.ApiErrorCode;
import com.gk.openapi.error.ApiException;
import com.gk.openapi.log.MerchantRequestLogger;
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
    private final MerchantRequestLogger merchantRequestLogger;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    /**
     * 查询余额
     * @return 余额
     */
    @PostMapping("balance")
    public ApiR<List<BalanceResponse>> balance(HttpServletRequest request) {
        long startMs = System.currentTimeMillis();
        try {
            BalanceQueryRequest body = bindSignParams(request, BalanceQueryRequest.class, false);
            List<BalanceResponse> data = openBalanceService.list(body.getCurrency());
            ApiR<List<BalanceResponse>> response = ApiR.success(data);
            merchantRequestLogger.balanceSuccess(request, body, data, startMs);
            return response;
        } catch (ApiException ex) {
            merchantRequestLogger.balanceFailed(request, ex, startMs);
            throw ex;
        } catch (Exception ex) {
            merchantRequestLogger.balanceFailed(request, ex, startMs);
            throw toRuntimeException(ex);
        }
    }

    @PostMapping( "pay/create")
    public ApiR<PayOrderResponse> createPay(HttpServletRequest request) {
        long startMs = System.currentTimeMillis();
        PayOrderCreateRequest body = null;
        try {
            body = bindSignParams(request, PayOrderCreateRequest.class, true);
            PayOrderResponse orderResp = openPayOrderService.create(body);
            ApiR<PayOrderResponse> response = ApiR.success(orderResp);
            merchantRequestLogger.payCreateSuccess(request, body, orderResp, response, startMs);
            return response;
        } catch (ApiException ex) {
            merchantRequestLogger.payCreateFailed(request, body, ex, startMs);
            throw ex;
        } catch (Exception ex) {
            merchantRequestLogger.payCreateFailed(request, body, ex, startMs);
            throw toRuntimeException(ex);
        }
    }

    @PostMapping( "pay/query")
    public ApiR<PayOrderResponse> queryPay(HttpServletRequest request) {
        long startMs = System.currentTimeMillis();
        PayOrderQueryRequest body = null;
        try {
            body = bindSignParams(request, PayOrderQueryRequest.class, false);
            PayOrderResponse orderResp = queryPayOrder(body);
            ApiR<PayOrderResponse> response = ApiR.success(orderResp);
            merchantRequestLogger.payQuerySuccess(request, body, orderResp, response, startMs);
            return response;
        } catch (ApiException ex) {
            merchantRequestLogger.payQueryFailed(request, body, ex, startMs);
            throw ex;
        } catch (Exception ex) {
            merchantRequestLogger.payQueryFailed(request, body, ex, startMs);
            throw toRuntimeException(ex);
        }
    }

    @PostMapping( "payout/create")
    public ApiR<PayoutOrderResponse> createPayout(HttpServletRequest request) {
        long startMs = System.currentTimeMillis();
        PayoutOrderCreateRequest body = null;
        try {
            body = bindSignParams(request, PayoutOrderCreateRequest.class, true);
            PayoutOrderResponse orderResp = openPayoutOrderService.create(body);
            ApiR<PayoutOrderResponse> response = ApiR.success(orderResp);
            merchantRequestLogger.payoutCreateSuccess(request, body, orderResp, response, startMs);
            return response;
        } catch (ApiException ex) {
            merchantRequestLogger.payoutCreateFailed(request, body, ex, startMs);
            throw ex;
        } catch (Exception ex) {
            merchantRequestLogger.payoutCreateFailed(request, body, ex, startMs);
            throw toRuntimeException(ex);
        }
    }

    @PostMapping("payout/query")
    public ApiR<PayoutOrderResponse> queryPayout(HttpServletRequest request) {
        long startMs = System.currentTimeMillis();
        PayoutOrderQueryRequest body = null;
        try {
            body = bindSignParams(request, PayoutOrderQueryRequest.class, false);
            PayoutOrderResponse orderResp = queryPayoutOrder(body);
            ApiR<PayoutOrderResponse> response = ApiR.success(orderResp);
            merchantRequestLogger.payoutQuerySuccess(request, body, orderResp, response, startMs);
            return response;
        } catch (ApiException ex) {
            merchantRequestLogger.payoutQueryFailed(request, body, ex, startMs);
            throw ex;
        } catch (Exception ex) {
            merchantRequestLogger.payoutQueryFailed(request, body, ex, startMs);
            throw toRuntimeException(ex);
        }
    }

    /**
     * 查询商户可展示的系统标准支付方式�?     *
     * <p>该接口只返回 payment_method 中配置的标准 method_code�?     * 实际下单可用通道仍以下单时的费率、支付计划和 PSP 路由匹配结果为准�?/p>
     */
    @PostMapping("methods")
    public ApiR<List<PaymentMethodResponse>> methods(HttpServletRequest request) {
        PaymentMethodQueryRequest body = bindSignParams(request, PaymentMethodQueryRequest.class, false);
        return ApiR.success(openPaymentMethodService.list(body.getCountryCode(), body.getCurrency(), body.getDirection()));
    }

    private <T> T bindSignParams(HttpServletRequest request, Class<T> requestType, boolean validate) {
        Object value = request.getAttribute(OpenApiAuthFilter.ATTR_SIGN_PARAMS);
        if (!(value instanceof Map<?, ?> params)) {
            throw new ApiException(ApiErrorCode.INVALID_REQUEST, "request parameters is empty");
        }
        T body = objectMapper.convertValue(params, requestType);
        if (validate) {
            Set<ConstraintViolation<T>> violations = validator.validate(body);
            if (!violations.isEmpty()) {
                throw new ApiException(ApiErrorCode.INVALID_REQUEST, violations.iterator().next().getMessage());
            }
        }
        return body;
    }

    private RuntimeException toRuntimeException(Exception ex) {
        if (ex instanceof RuntimeException runtimeException) {
            return runtimeException;
        }
        return new ApiException(ApiErrorCode.SYSTEM_ERROR, ex);
    }

    private PayOrderResponse queryPayOrder(PayOrderQueryRequest body) {
        if (StringUtils.isNotBlank(body.getSystemOrderId())) {
            return openPayOrderService.getByPayOrderNo(body.getSystemOrderId());
        }
        if (StringUtils.isNotBlank(body.getMerchantOrderId())) {
            return openPayOrderService.getByMerchantOrderNo(body.getMerchantOrderId());
        }
        throw new ApiException(ApiErrorCode.INVALID_REQUEST, "system_order_id or merchant_order_id is required");
    }

    private PayoutOrderResponse queryPayoutOrder(PayoutOrderQueryRequest body) {
        if (StringUtils.isNotBlank(body.getSystemOrderId())) {
            return openPayoutOrderService.getByPayoutOrderNo(body.getSystemOrderId());
        }
        if (StringUtils.isNotBlank(body.getMerchantOrderId())) {
            return openPayoutOrderService.getByMerchantOrderNo(body.getMerchantOrderId());
        }
        throw new ApiException(ApiErrorCode.INVALID_REQUEST, "system_order_id or merchant_order_id is required");
    }
}
