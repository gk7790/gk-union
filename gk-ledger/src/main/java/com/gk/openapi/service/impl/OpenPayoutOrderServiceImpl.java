package com.gk.openapi.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gk.openapi.dto.PayoutOrderCreateRequest;
import com.gk.openapi.dto.PayoutOrderResponse;
import com.gk.openapi.error.ApiErrorCode;
import com.gk.openapi.error.OpenApiException;
import com.gk.openapi.security.OpenApiRequestContextHolder;
import com.gk.openapi.service.OpenPayoutOrderService;
import com.gk.payment.dao.PayoutOrderDao;
import com.gk.payment.entity.PayoutOrderEntity;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OpenPayoutOrderServiceImpl implements OpenPayoutOrderService {
    private final PayoutOrderDao payoutOrderDao;

    @Override
    public PayoutOrderResponse create(PayoutOrderCreateRequest request, String idempotencyKey) {
        if (StringUtils.isBlank(idempotencyKey)) {
            throw new OpenApiException(ApiErrorCode.INVALID_REQUEST, "Missing header: X-Idempotency-Key");
        }
        throw new OpenApiException(ApiErrorCode.SERVICE_NOT_READY, "Payout order creation flow is not wired yet");
    }

    @Override
    public PayoutOrderResponse getByPayoutOrderNo(String payoutOrderNo) {
        PayoutOrderEntity entity = payoutOrderDao.selectOne(baseWrapper().eq("payout_order_no", payoutOrderNo).last("limit 1"));
        return toResponse(entity);
    }

    @Override
    public PayoutOrderResponse getByMerchantOrderNo(String merchantOrderNo) {
        PayoutOrderEntity entity = payoutOrderDao.selectOne(baseWrapper().eq("merchant_order_no", merchantOrderNo).last("limit 1"));
        return toResponse(entity);
    }

    private QueryWrapper<PayoutOrderEntity> baseWrapper() {
        return new QueryWrapper<PayoutOrderEntity>()
                .eq("tenant_id", OpenApiRequestContextHolder.getTenantId())
                .eq("merchant_id", OpenApiRequestContextHolder.getMerchantId());
    }

    private PayoutOrderResponse toResponse(PayoutOrderEntity entity) {
        if (entity == null) {
            throw new OpenApiException(ApiErrorCode.ORDER_NOT_FOUND);
        }
        PayoutOrderResponse response = new PayoutOrderResponse();
        response.setPayoutOrderNo(entity.getPayoutOrderNo());
        response.setMerchantOrderNo(entity.getMerchantOrderNo());
        response.setStatus(entity.getStatus());
        response.setStatusReason(entity.getStatusReason());
        response.setAmount(entity.getAmount());
        response.setCurrency(entity.getCurrency());
        response.setCountryCode(entity.getCountryCode());
        response.setMethodCode(entity.getMethodCode());
        response.setPspOrderNo(entity.getPspOrderNo());
        return response;
    }
}
