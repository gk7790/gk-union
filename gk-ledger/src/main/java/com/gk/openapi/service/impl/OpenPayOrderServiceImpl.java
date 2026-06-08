package com.gk.openapi.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gk.common.utils.BizKeyUtils;
import com.gk.merchant.entity.MerchantEntity;
import com.gk.openapi.dto.PayOrderCreateRequest;
import com.gk.openapi.dto.PayOrderResponse;
import com.gk.openapi.error.ApiErrorCode;
import com.gk.openapi.error.ApiException;
import com.gk.openapi.security.ApiReqContext;
import com.gk.openapi.security.ApiReqContextHolder;
import com.gk.openapi.service.OpenPayOrderService;
import com.gk.payment.dao.PayOrderDao;
import com.gk.payment.entity.PayOrderEntity;
import com.gk.payment.fee.MerchantFeeResult;
import com.gk.payment.service.MerchantFeeRuleService;
import com.gk.psp.dispatch.PspPayDispatchResult;
import com.gk.psp.dispatch.PspPayDispatchService;
import com.gk.psp.fee.PspFeeResult;
import com.gk.psp.route.PspRouteResult;
import com.gk.psp.route.PspRouteSelector;
import com.gk.psp.service.PspFeeRuleService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OpenPayOrderServiceImpl implements OpenPayOrderService {
    private static final String ORDER_SOURCE_API = "API";
    private static final String STATUS_CREATED = "CREATED";
    private static final String STATUS_PROCESSING = "PROCESSING";
    private static final String STATUS_FAILED = "FAILED";
    private static final String SETTLE_STATUS_PENDING = "PENDING";

    private final PayOrderDao payOrderDao;
    private final MerchantFeeRuleService merchantFeeRuleService;
    private final PspRouteSelector pspRouteSelector;
    private final PspFeeRuleService pspFeeRuleService;
    private final PspPayDispatchService pspPayDispatchService;
    private final ObjectMapper objectMapper;

    @Override
    public PayOrderResponse create(PayOrderCreateRequest request) {
        PayOrderEntity existed = payOrderDao.selectOne(
                baseWrapper()
                        .eq("merchant_order_no", request.getMerchantOrderNo())
                        .last("limit 1")
        );

        if (existed != null) {
            return toResponse(existed);
        }

        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiException(ApiErrorCode.INVALID_AMOUNT);
        }

        ApiReqContext context = ApiReqContextHolder.get();
        MerchantEntity merchant = context.getMerchant();

        String currency = StringUtils.defaultIfBlank(request.getCurrency(), merchant.getDefaultCurrency());
        String countryCode = StringUtils.defaultIfBlank(request.getCountryCode(), merchant.getCountryCode());
        if (StringUtils.isBlank(currency) || StringUtils.isBlank(countryCode)) {
            throw new ApiException(ApiErrorCode.INVALID_REQUEST, "currency and country_code is required");
        }

        PayOrderEntity entity = new PayOrderEntity();
        entity.setTenantId(context.getTenantId());
        entity.setMerchantId(context.getMerchantId());
        entity.setMerchantNo(context.getMerchantNo());
        entity.setMerchantAppId(context.getMerchantAppId());
        entity.setAppId(context.getAppId());
        entity.setPayOrderNo(BizKeyUtils.genPayOrderNo());
        entity.setMerchantOrderNo(request.getMerchantOrderNo());
        entity.setIdempotencyKey(request.getMerchantOrderNo());
        entity.setOrderSource(ORDER_SOURCE_API);
        entity.setCountryCode(countryCode.toUpperCase(Locale.ROOT));
        entity.setCurrency(currency.toUpperCase(Locale.ROOT));
        entity.setMethodCode(request.getMethodCode().toUpperCase(Locale.ROOT));
        entity.setAmount(request.getAmount());
        entity.setPaidAmount(BigDecimal.ZERO);
        entity.setPspFeeAmount(BigDecimal.ZERO);
        entity.setSubject(request.getSubject());
        entity.setDescription(request.getDescription());
        entity.setClientIp(context.getClientIp());
        entity.setPayerJson(toJson(request.getPayer()));
        entity.setNotifyUrl(request.getNotifyUrl());
        entity.setReturnUrl(request.getReturnUrl());
        entity.setStatus(STATUS_CREATED);
        entity.setSettleStatus(SETTLE_STATUS_PENDING);
        entity.setExtraJson(toJson(request.getExtra()));
        entity.setVersion(0);

        applyMerchantFee(entity);

        boolean created = insertOrder(entity);
        if (!created) {
            return toResponse(entity);
        }
        submitToPsp(entity);
        return toResponse(entity);
    }

    private boolean insertOrder(PayOrderEntity entity) {
        try {
            payOrderDao.insert(entity);
            return true;
        } catch (DuplicateKeyException ex) {
            PayOrderEntity existed = payOrderDao.selectOne(
                    baseWrapper()
                            .eq("merchant_order_no", entity.getMerchantOrderNo())
                            .last("limit 1")
            );
            if (existed != null) {
                copyOrder(existed, entity);
                return false;
            }
            throw ex;
        }
    }

    private void submitToPsp(PayOrderEntity entity) {
        try {
            PspRouteResult route = pspRouteSelector.selectPayin(entity);
            applyRoute(entity, route);
            applyPspFee(entity);

            PspPayDispatchResult dispatchResult = pspPayDispatchService.dispatch(entity, route);
            applyDispatchResult(entity, dispatchResult);
            payOrderDao.updateById(entity);
        } catch (ApiException ex) {
            markFailed(entity, ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            markFailed(entity, ApiErrorCode.SYSTEM_ERROR.getMessage());
            throw new ApiException(ApiErrorCode.SYSTEM_ERROR);
        }
    }

    private void applyRoute(PayOrderEntity entity, PspRouteResult route) {
        entity.setRouteRuleId(route.getRouteRuleId());
        entity.setPspId(route.getPspId());
        entity.setPspCode(route.getPspCode());
        entity.setPspMethodId(route.getPspMethodId());
        entity.setPspMethodCode(route.getPspMethodCode());
        entity.setPspAccountId(route.getPspAccountId());
        entity.setPspAccountNo(route.getPspAccountNo());
    }

    private void applyDispatchResult(PayOrderEntity entity, PspPayDispatchResult result) {
        entity.setPspRequestNo(result.getPspRequestNo());
        entity.setPspOrderNo(result.getPspOrderNo());
        entity.setPspPayUrl(result.getPayUrl());
        entity.setPspRawStatus(result.getRawStatus());
        if (result.isSuccess()) {
            entity.setStatus(STATUS_PROCESSING);
            entity.setPspStatus(STATUS_PROCESSING);
            return;
        }
        entity.setStatus(STATUS_FAILED);
        entity.setPspStatus(STATUS_FAILED);
        entity.setStatusReason(StringUtils.defaultIfBlank(result.getErrorMessage(), "PSP submit failed"));
    }

    private void markFailed(PayOrderEntity entity, String reason) {
        entity.setStatus(STATUS_FAILED);
        entity.setStatusReason(StringUtils.defaultIfBlank(reason, "Pay order failed"));
        payOrderDao.updateById(entity);
    }

    private String toJson(Map<String, Object> value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new ApiException(ApiErrorCode.INVALID_REQUEST, "Invalid JSON field");
        }
    }

    private void copyOrder(PayOrderEntity source, PayOrderEntity target) {
        target.setId(source.getId());
        target.setTenantId(source.getTenantId());
        target.setMerchantId(source.getMerchantId());
        target.setMerchantNo(source.getMerchantNo());
        target.setMerchantAppId(source.getMerchantAppId());
        target.setAppId(source.getAppId());
        target.setPayOrderNo(source.getPayOrderNo());
        target.setMerchantOrderNo(source.getMerchantOrderNo());
        target.setStatus(source.getStatus());
        target.setStatusReason(source.getStatusReason());
        target.setAmount(source.getAmount());
        target.setPaidAmount(source.getPaidAmount());
        target.setMerchantFeeAmount(source.getMerchantFeeAmount());
        target.setPspFeeAmount(source.getPspFeeAmount());
        target.setPspFeeRuleId(source.getPspFeeRuleId());
        target.setPspFeeSnapshotJson(source.getPspFeeSnapshotJson());
        target.setSettleAmount(source.getSettleAmount());
        target.setFeeRuleId(source.getFeeRuleId());
        target.setFeeSnapshotJson(source.getFeeSnapshotJson());
        target.setCurrency(source.getCurrency());
        target.setCountryCode(source.getCountryCode());
        target.setMethodCode(source.getMethodCode());
        target.setPspPayUrl(source.getPspPayUrl());
        target.setPspOrderNo(source.getPspOrderNo());
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
                .eq("tenant_id", ApiReqContextHolder.getTenantId())
                .eq("merchant_id", ApiReqContextHolder.getMerchantId());
    }

    private void applyMerchantFee(PayOrderEntity entity) {
        MerchantFeeResult feeResult = merchantFeeRuleService.calculatePayin(entity);
        entity.setMerchantFeeAmount(feeResult.getMerchantFeeAmount());
        entity.setSettleAmount(feeResult.getSettleAmount());
        entity.setFeeRuleId(feeResult.getRule().getId());
        entity.setFeeSnapshotJson(feeResult.getSnapshotJson());
    }

    private void applyPspFee(PayOrderEntity entity) {
        PspFeeResult feeResult = pspFeeRuleService.calculatePayin(entity);
        entity.setPspFeeAmount(feeResult.getPspFeeAmount());
        entity.setPspFeeRuleId(feeResult.getRule().getId());
        entity.setPspFeeSnapshotJson(feeResult.getSnapshotJson());
    }

    private PayOrderResponse toResponse(PayOrderEntity entity) {
        if (entity == null) {
            throw new ApiException(ApiErrorCode.ORDER_NOT_FOUND);
        }
        PayOrderResponse response = new PayOrderResponse();
        response.setPayOrderNo(entity.getPayOrderNo());
        response.setMerchantOrderNo(entity.getMerchantOrderNo());
        response.setStatus(entity.getStatus());
        response.setStatusReason(entity.getStatusReason());
        response.setAmount(entity.getAmount());
        response.setPaidAmount(entity.getPaidAmount());
        response.setMerchantFeeAmount(entity.getMerchantFeeAmount());
        response.setSettleAmount(entity.getSettleAmount());
        response.setCurrency(entity.getCurrency());
        response.setCountryCode(entity.getCountryCode());
        response.setMethodCode(entity.getMethodCode());
        response.setPayUrl(entity.getPspPayUrl());
        response.setPspOrderNo(entity.getPspOrderNo());
        return response;
    }
}
