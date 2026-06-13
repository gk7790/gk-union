package com.gk.openapi.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gk.common.utils.BizKeyUtils;
import com.gk.merchant.entity.MerchantAppEntity;
import com.gk.merchant.entity.MerchantEntity;
import com.gk.openapi.dto.PayOrderCreateRequest;
import com.gk.openapi.dto.PayOrderResponse;
import com.gk.openapi.error.ApiErrorCode;
import com.gk.openapi.error.ApiException;
import com.gk.openapi.security.ApiReqContext;
import com.gk.openapi.security.ApiReqContextHolder;
import com.gk.openapi.service.OpenPayOrderService;
import com.gk.openapi.util.ApiAmountUtils;
import com.gk.common.enums.OrderSourceEnum;
import com.gk.payment.enums.PayOrderStatusEnum;
import com.gk.payment.dao.PayOrderDao;
import com.gk.payment.entity.PayOrderEntity;
import com.gk.payment.enums.SettleStatusEnum;
import com.gk.payment.fee.MerchantFeeResult;
import com.gk.payment.notify.MerchantOrderNotifyStatusService;
import com.gk.payment.service.MerchantFeeRuleService;
import com.gk.payment.service.OrderStatusLogService;
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
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OpenPayOrderServiceImpl implements OpenPayOrderService {

    private final PayOrderDao payOrderDao;
    private final MerchantFeeRuleService merchantFeeRuleService;
    private final PspRouteSelector pspRouteSelector;
    private final PspFeeRuleService pspFeeRuleService;
    private final PspPayDispatchService pspPayDispatchService;
    private final ObjectMapper objectMapper;
    private final MerchantOrderNotifyStatusService merchantOrderNotifyStatusService;
    private final OrderStatusLogService orderStatusLogService;

    @Override
    public PayOrderResponse create(PayOrderCreateRequest request) {
        PayOrderEntity existed = payOrderDao.selectOne(
                baseWrapper()
                        .eq("merchant_order_no", StringUtils.trim(request.getMerchantOrderId()))
                        .last("limit 1")
        );

        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiException(ApiErrorCode.INVALID_AMOUNT);
        }
        validateAmountScale(request.getAmount());

        ApiReqContext context = ApiReqContextHolder.get();
        MerchantEntity merchant = context.getMerchant();

        String currency = StringUtils.defaultIfBlank(request.getCurrency(), merchant.getDefaultCurrency());
        String countryCode = StringUtils.defaultIfBlank(request.getCountryCode(), merchant.getCountryCode());
        if (StringUtils.isBlank(currency) || StringUtils.isBlank(countryCode)) {
            throw new ApiException(ApiErrorCode.INVALID_REQUEST, "currency and country_code is required");
        }
        String normalizedCurrency = currency.toUpperCase(Locale.ROOT);
        String normalizedMethod = request.getMethodCode().toUpperCase(Locale.ROOT);
        validateMerchantAppAccess(context.getMerchantApp(), normalizedCurrency, normalizedMethod);

        if (existed != null) {
            validateIdempotentRequest(existed, request, normalizedCurrency, normalizedMethod);
            return toResponse(existed);
        }

        PayOrderEntity entity = new PayOrderEntity();
        entity.setTenantId(context.getTenantId());
        entity.setMerchantId(context.getMerchantId());
        entity.setMerchantNo(context.getMerchantNo());
        entity.setMerchantAppId(context.getMerchantAppId());
        entity.setAppId(context.getAppId());
        entity.setPayOrderNo(BizKeyUtils.genPayOrderNo());
        entity.setMerchantOrderNo(StringUtils.trim(request.getMerchantOrderId()));
        entity.setIdempotencyKey(StringUtils.trim(request.getMerchantOrderId()));
        entity.setOrderSource(OrderSourceEnum.API.code());
        entity.setCountryCode(countryCode.toUpperCase(Locale.ROOT));
        entity.setCurrency(normalizedCurrency);
        entity.setMethodCode(normalizedMethod);
        entity.setAmount(request.getAmount());
        entity.setPaidAmount(BigDecimal.ZERO);
        entity.setPspFeeAmount(BigDecimal.ZERO);
        entity.setSubject(request.getSubject());
        entity.setDescription(request.getDescription());
        entity.setClientIp(context.getClientIp());
        entity.setPayerJson(toJson(request.getPayer()));
        entity.setNotifyUrl(request.getNotifyUrl());
        entity.setReturnUrl(request.getReturnUrl());
        entity.setMerchantNotifyStatus(merchantOrderNotifyStatusService.initialStatus(entity.getNotifyUrl()));
        entity.setStatus(PayOrderStatusEnum.CREATED.code());
        entity.setSettleStatus(SettleStatusEnum.PENDING.code());
        entity.setQueryCount(0);
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
            recordStatusChange(entity, null, entity.getStatus(), "ORDER_CREATED", null, "MERCHANT");
            return true;
        } catch (DuplicateKeyException ex) {
            PayOrderEntity existed = payOrderDao.selectOne(
                    baseWrapper()
                            .eq("merchant_order_no", entity.getMerchantOrderNo())
                            .last("limit 1")
            );
            if (existed != null) {
                validateIdempotentEntity(existed, entity);
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
        String fromStatus = entity.getStatus();
        entity.setPspRequestNo(result.getPspRequestNo());
        entity.setPspOrderNo(result.getPspOrderNo());
        entity.setPspPayUrl(result.getPayUrl());
        entity.setPspPayParamsJson(result.getPayParamsJson());
        entity.setPspRawStatus(result.getRawStatus());
        if (result.isSuccess()) {
            entity.setStatus(PayOrderStatusEnum.PROCESSING.code());
            entity.setPspStatus(PayOrderStatusEnum.PROCESSING.code());
            entity.setNextQueryAt(Instant.now().plusSeconds(60));
            recordStatusChange(entity, fromStatus, entity.getStatus(), "PSP_SUBMIT", null, "SYSTEM");
            return;
        }
        entity.setStatus(PayOrderStatusEnum.FAILED.code());
        entity.setPspStatus(PayOrderStatusEnum.FAILED.code());
        String reason = StringUtils.defaultIfBlank(
                result.getErrorMessage(),
                StringUtils.defaultIfBlank(result.getResponseMessage(), "PSP submit failed")
        );
        entity.setStatusReason(reason);
        recordStatusChange(entity, fromStatus, entity.getStatus(), "PSP_SUBMIT_FAILED", reason, "SYSTEM");
    }

    /**
     * 标记失败状态和失败原因
     * @param entity 订单
     * @param reason 失败原因
     */
    private void markFailed(PayOrderEntity entity, String reason) {
        String fromStatus = entity.getStatus();
        entity.setStatus(PayOrderStatusEnum.FAILED.code());
        String message = StringUtils.defaultIfBlank(reason, "Pay order failed");
        entity.setStatusReason(message);
        payOrderDao.updateById(entity);
        recordStatusChange(entity, fromStatus, entity.getStatus(), "ORDER_FAILED", message, "SYSTEM");
    }

    private void recordStatusChange(PayOrderEntity entity,
                                    String fromStatus,
                                    String toStatus,
                                    String eventType,
                                    String reason,
                                    String operatorType) {
        orderStatusLogService.recordChange(
                "PAY",
                entity.getTenantId(),
                entity.getMerchantId(),
                entity.getId(),
                entity.getPayOrderNo(),
                fromStatus,
                toStatus,
                eventType,
                reason,
                operatorType,
                ApiReqContextHolder.getAppId(),
                entity.getMerchantOrderNo(),
                traceId()
        );
    }

    private String traceId() {
        ApiReqContext context = ApiReqContextHolder.get();
        return context == null ? null : context.getTraceId();
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

    private void validateIdempotentRequest(PayOrderEntity existed, PayOrderCreateRequest request, String currency, String methodCode) {
        if (existed.getAmount() == null || request.getAmount() == null
                || existed.getAmount().compareTo(request.getAmount()) != 0
                || !StringUtils.equalsIgnoreCase(existed.getCurrency(), currency)
                || !StringUtils.equalsIgnoreCase(existed.getMethodCode(), methodCode)
                || !StringUtils.equals(StringUtils.trimToEmpty(existed.getNotifyUrl()), StringUtils.trimToEmpty(request.getNotifyUrl()))) {
            throw new ApiException(ApiErrorCode.DUPLICATE_REQUEST, "merchant_order_id exists with different request parameters");
        }
    }

    private void validateIdempotentEntity(PayOrderEntity existed, PayOrderEntity entity) {
        if (existed.getAmount() == null || entity.getAmount() == null
                || existed.getAmount().compareTo(entity.getAmount()) != 0
                || !StringUtils.equalsIgnoreCase(existed.getCurrency(), entity.getCurrency())
                || !StringUtils.equalsIgnoreCase(existed.getMethodCode(), entity.getMethodCode())
                || !StringUtils.equals(StringUtils.trimToEmpty(existed.getNotifyUrl()), StringUtils.trimToEmpty(entity.getNotifyUrl()))) {
            throw new ApiException(ApiErrorCode.DUPLICATE_REQUEST, "merchant_order_id exists with different request parameters");
        }
    }

    private void validateMerchantAppAccess(MerchantAppEntity app, String currency, String methodCode) {
        if (!allowed(app == null ? null : app.getAllowedCurrencyJson(), currency)) {
            throw new ApiException(ApiErrorCode.INVALID_REQUEST, "currency is not allowed for app");
        }
        if (!allowed(app == null ? null : app.getAllowedMethodJson(), methodCode)) {
            throw new ApiException(ApiErrorCode.UNSUPPORTED_METHOD, "method is not allowed for app");
        }
    }

    private boolean allowed(String jsonArray, String value) {
        if (StringUtils.isBlank(jsonArray)) {
            return true;
        }
        try {
            List<String> allowedValues = objectMapper.readValue(jsonArray, objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
            return allowedValues.stream().anyMatch(item -> StringUtils.equalsIgnoreCase(item, value));
        } catch (Exception ex) {
            throw new ApiException(ApiErrorCode.INVALID_REQUEST, "Invalid app allowed config");
        }
    }

    private void validateAmountScale(BigDecimal amount) {
        if (amount.scale() > 8) {
            throw new ApiException(ApiErrorCode.INVALID_AMOUNT, "amount scale must be less than or equal to 8");
        }
    }

    private PayOrderResponse toResponse(PayOrderEntity entity) {
        if (entity == null) {
            throw new ApiException(ApiErrorCode.ORDER_NOT_FOUND);
        }
        PayOrderResponse response = new PayOrderResponse();
        response.setSystemOrderId(entity.getPayOrderNo());
        response.setMerchantOrderId(entity.getMerchantOrderNo());
        response.setStatus(entity.getStatus());
        response.setStatusReason(entity.getStatusReason());
        response.setAmount(formatMoney(entity.getAmount(), entity.getCurrency()));
        response.setCurrency(entity.getCurrency());
        response.setCountryCode(entity.getCountryCode());
        response.setMethodCode(entity.getMethodCode());
        response.setPayUrl(entity.getPspPayUrl());
        return response;
    }

    private String formatMoney(BigDecimal value, String currency) {
        return value == null ? null : ApiAmountUtils.formatCurrencyAmount(value, currency);
    }
}
