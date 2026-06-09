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
import com.gk.openapi.util.ApiAmountUtils;
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

    /**
     * 提交到三方路由
     * @param order 订单
     */
    private void submitToPsp(PayOrderEntity order) {
        try {
            PspRouteResult route = pspRouteSelector.selectPayin(order);
            // 路由PSP
            applyRoute(order, route);

            // 设置psp手续费
            applyPspFee(order);

            PspPayDispatchResult dispatchResult = pspPayDispatchService.dispatch(order, route);

            applyDispatchResult(order, dispatchResult);

            payOrderDao.updateById(order);
        } catch (ApiException ex) {
            markFailed(order, ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            markFailed(order, ApiErrorCode.SYSTEM_ERROR.getMessage());
            throw new ApiException(ApiErrorCode.SYSTEM_ERROR);
        }
    }

    /**
     * 为订单路由三方PSP
     * @param entity 订单
     * @param route 路由
     */
    private void applyRoute(PayOrderEntity entity, PspRouteResult route) {
        entity.setRouteRuleId(route.getRouteRuleId());
        entity.setPspId(route.getPspId());
        entity.setPspCode(route.getPspCode());
        entity.setPspMethodId(route.getPspMethodId());
        entity.setPspMethodCode(route.getPspMethodCode());
        entity.setPspAccountId(route.getPspAccountId());
        entity.setPspAccountNo(route.getPspAccountNo());
    }

    /**
     * PSP 发送请求成功
     * @param entity 订单
     * @param result 请求结果
     */
    private void applyDispatchResult(PayOrderEntity entity, PspPayDispatchResult result) {
        entity.setPspRequestNo(result.getPspRequestNo());
        entity.setPspOrderNo(result.getPspOrderNo());
        entity.setPspPayUrl(result.getPayUrl());
        entity.setPspPayParamsJson(result.getPayParamsJson());
        entity.setPspRawStatus(result.getRawStatus());
        if (result.isSuccess()) {
            entity.setStatus(STATUS_PROCESSING);
            entity.setPspStatus(STATUS_PROCESSING);
            return;
        }
        entity.setStatus(STATUS_FAILED);
        entity.setPspStatus(STATUS_FAILED);
        entity.setStatusReason(StringUtils.defaultIfBlank(
                result.getErrorMessage(),
                StringUtils.defaultIfBlank(result.getResponseMessage(), "PSP submit failed")
        ));
    }

    /**
     * 标记失败状态和失败原因
     * @param entity 订单
     * @param reason 失败原因
     */
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
        target.setMerchantFeeRuleId(source.getMerchantFeeRuleId());
        target.setMerchantFeeSnapshotJson(source.getMerchantFeeSnapshotJson());
        target.setPspFeeAmount(source.getPspFeeAmount());
        target.setPspFeeRuleId(source.getPspFeeRuleId());
        target.setPspFeeSnapshotJson(source.getPspFeeSnapshotJson());
        target.setSettleAmount(source.getSettleAmount());
        target.setCurrency(source.getCurrency());
        target.setCountryCode(source.getCountryCode());
        target.setMethodCode(source.getMethodCode());
        target.setPspPayUrl(source.getPspPayUrl());
        target.setPspOrderNo(source.getPspOrderNo());
    }

    @Override
    public PayOrderResponse getByPayOrderNo(String payOrderNo) {
        PayOrderEntity entity = payOrderDao.selectOne(baseWrapper().eq("pay_order_no", StringUtils.trim(payOrderNo)).last("limit 1"));
        return toResponse(entity);
    }

    @Override
    public PayOrderResponse getByMerchantOrderNo(String merchantOrderNo) {
        PayOrderEntity entity = payOrderDao.selectOne(baseWrapper().eq("merchant_order_no", StringUtils.trim(merchantOrderNo)).last("limit 1"));
        return toResponse(entity);
    }

    /**
     * 必带有参数
     * @return 返回筛选条件
     */
    private QueryWrapper<PayOrderEntity> baseWrapper() {
        return new QueryWrapper<PayOrderEntity>()
                .eq("tenant_id", ApiReqContextHolder.getTenantId())
                .eq("merchant_id", ApiReqContextHolder.getMerchantId());
    }

    /**
     * 天啊及 商户手续费, 并计算结算金额, 路由Id, 路由快照
     * @param entity 订单
     */
    private void applyMerchantFee(PayOrderEntity entity) {
        MerchantFeeResult feeResult = merchantFeeRuleService.calculatePayin(entity);
        entity.setMerchantFeeAmount(feeResult.getMerchantFeeAmount());
        entity.setSettleAmount(feeResult.getSettleAmount());
        entity.setMerchantFeeRuleId(feeResult.getRule().getId());
        entity.setMerchantFeeSnapshotJson(feeResult.getSnapshotJson());
    }

    /**
     * 添加 PSP手续费, 路由Id, 路由快照
     * @param entity 订单
     */
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
        response.setAmount(formatMoney(entity.getAmount(), entity.getCurrency()));
        response.setCurrency(entity.getCurrency());
        response.setCountryCode(entity.getCountryCode());
        response.setMethodCode(entity.getMethodCode());
        response.setPayUrl(entity.getPspPayUrl());
        response.setPspOrderNo(entity.getPspOrderNo());
        return response;
    }

    private String formatMoney(BigDecimal value, String currency) {
        return value == null ? null : ApiAmountUtils.formatCurrencyAmount(value, currency);
    }
}
