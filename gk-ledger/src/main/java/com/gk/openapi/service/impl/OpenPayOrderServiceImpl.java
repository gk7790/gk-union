package com.gk.openapi.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gk.openapi.dto.PayOrderCreateRequest;
import com.gk.openapi.dto.PayOrderResponse;
import com.gk.openapi.error.ApiErrorCode;
import com.gk.openapi.error.OpenApiException;
import com.gk.openapi.security.OpenApiRequestContextHolder;
import com.gk.openapi.service.OpenPayOrderService;
import com.gk.payment.dao.PayOrderDao;
import com.gk.payment.entity.PayOrderEntity;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OpenPayOrderServiceImpl implements OpenPayOrderService {
    private final PayOrderDao payOrderDao;

    @Override
    public PayOrderResponse create(PayOrderCreateRequest request, String idempotencyKey) {
        if (StringUtils.isBlank(idempotencyKey)) {
            throw new OpenApiException(ApiErrorCode.INVALID_REQUEST, "Missing header: X-Idempotency-Key");
        }
        throw new OpenApiException(ApiErrorCode.SERVICE_NOT_READY, "Pay order creation flow is not wired yet");
    }

    @Override
    public PayOrderResponse getByPayOrderNo(String payOrderNo) {
        PayOrderEntity entity = payOrderDao.selectOne(baseWrapper().eq("pay_order_no", payOrderNo).last("limit 1"));
        return toResponse(entity);
    }

    @Override
    public PayOrderResponse getByMerchantOrderNo(String merchantOrderNo) {
        PayOrderEntity entity = payOrderDao.selectOne(baseWrapper().eq("merchant_order_no", merchantOrderNo).last("limit 1"));
        return toResponse(entity);
    }

    private QueryWrapper<PayOrderEntity> baseWrapper() {
        return new QueryWrapper<PayOrderEntity>()
                .eq("tenant_id", OpenApiRequestContextHolder.getTenantId())
                .eq("merchant_id", OpenApiRequestContextHolder.getMerchantId());
    }

    private PayOrderResponse toResponse(PayOrderEntity entity) {
        if (entity == null) {
            throw new OpenApiException(ApiErrorCode.ORDER_NOT_FOUND);
        }
        PayOrderResponse response = new PayOrderResponse();
        response.setPayOrderNo(entity.getPayOrderNo());
        response.setMerchantOrderNo(entity.getMerchantOrderNo());
        response.setStatus(entity.getStatus());
        response.setStatusReason(entity.getStatusReason());
        response.setAmount(entity.getAmount());
        response.setPaidAmount(entity.getPaidAmount());
        response.setCurrency(entity.getCurrency());
        response.setCountryCode(entity.getCountryCode());
        response.setMethodCode(entity.getMethodCode());
        response.setPayUrl(entity.getPspPayUrl());
        response.setPspOrderNo(entity.getPspOrderNo());
        return response;
    }
}
