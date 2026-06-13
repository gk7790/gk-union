package com.gk.openapi.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gk.common.utils.BizKeyUtils;
import com.gk.ledger.posting.LedgerPostingResult;
import com.gk.ledger.posting.PayoutPostingRequest;
import com.gk.ledger.service.LedgerPostingService;
import com.gk.merchant.entity.MerchantAppEntity;
import com.gk.merchant.entity.MerchantEntity;
import com.gk.openapi.dto.PayoutOrderCreateRequest;
import com.gk.openapi.dto.PayoutOrderResponse;
import com.gk.openapi.error.ApiErrorCode;
import com.gk.openapi.error.ApiException;
import com.gk.openapi.security.ApiReqContext;
import com.gk.openapi.security.ApiReqContextHolder;
import com.gk.openapi.service.OpenPayoutOrderService;
import com.gk.openapi.util.ApiAmountUtils;
import com.gk.common.enums.OrderSourceEnum;
import com.gk.payment.payout.enums.PayoutOrderStatusEnum;
import com.gk.payment.payout.dao.PayoutOrderDao;
import com.gk.payment.payout.entity.PayoutOrderEntity;
import com.gk.payment.fee.MerchantFeeResult;
import com.gk.payment.notify.MerchantOrderNotifyStatusService;
import com.gk.payment.service.MerchantFeeRuleService;
import com.gk.payment.service.OrderStatusLogService;
import com.gk.psp.dispatch.PspPayoutDispatchResult;
import com.gk.psp.dispatch.PspPayoutDispatchService;
import com.gk.psp.fee.PspFeeResult;
import com.gk.psp.route.PspRouteResult;
import com.gk.psp.route.PspRouteSelector;
import com.gk.psp.service.PspFeeRuleService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OpenPayoutOrderServiceImpl implements OpenPayoutOrderService {

    private final PayoutOrderDao payoutOrderDao;
    private final MerchantFeeRuleService merchantFeeRuleService;
    private final PspRouteSelector pspRouteSelector;
    private final PspFeeRuleService pspFeeRuleService;
    private final PspPayoutDispatchService pspPayoutDispatchService;
    private final LedgerPostingService ledgerPostingService;
    private final ObjectMapper objectMapper;
    private final MerchantOrderNotifyStatusService merchantOrderNotifyStatusService;
    private final OrderStatusLogService orderStatusLogService;

    @Override
    public PayoutOrderResponse create(PayoutOrderCreateRequest request) {
        PayoutOrderEntity existed = payoutOrderDao.selectOne(
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

        PayoutOrderEntity entity = new PayoutOrderEntity();
        entity.setTenantId(context.getTenantId());
        entity.setMerchantId(context.getMerchantId());
        entity.setMerchantNo(context.getMerchantNo());
        entity.setMerchantAppId(context.getMerchantAppId());
        entity.setAppId(context.getAppId());
        entity.setPayoutOrderNo(BizKeyUtils.genPayoutOrderNo());
        entity.setMerchantOrderNo(StringUtils.trim(request.getMerchantOrderId()));
        entity.setIdempotencyKey(StringUtils.trim(request.getMerchantOrderId()));
        entity.setOrderSource(OrderSourceEnum.API.code());
        entity.setCountryCode(countryCode.toUpperCase(Locale.ROOT));
        entity.setCurrency(normalizedCurrency);
        entity.setMethodCode(normalizedMethod);
        entity.setAmount(request.getAmount());
        entity.setMerchantFeeAmount(BigDecimal.ZERO);
        entity.setTotalDebitAmount(request.getAmount());
        entity.setPspFeeAmount(BigDecimal.ZERO);
        entity.setPurpose(StringUtils.trimToNull(request.getPurpose()));
        entity.setNotifyUrl(StringUtils.trimToNull(request.getNotifyUrl()));
        entity.setMerchantNotifyStatus(merchantOrderNotifyStatusService.initialStatus(entity.getNotifyUrl()));
        entity.setStatus(PayoutOrderStatusEnum.CREATED.code());
        entity.setQueryCount(0);
        entity.setExtraJson(toJson(request.getExtra()));
        entity.setVersion(0);

        applyPayee(entity, request);
        applyMerchantFee(entity);

        boolean created = insertOrder(entity);
        if (!created) {
            return toResponse(entity);
        }
        try {
            // 冻结账户金额
            freezePayout(entity);
        } catch (ApiException ex) {
            markFailed(entity, ex.getMessage(), ex.getErrorCode().name());
            throw ex;
        } catch (Exception ex) {
            ApiErrorCode errorCode = StringUtils.containsIgnoreCase(ex.getMessage(), "Insufficient ledger balance")
                    ? ApiErrorCode.INSUFFICIENT_BALANCE
                    : ApiErrorCode.SYSTEM_ERROR;
            markFailed(entity, errorCode.getMessage(), errorCode.name());
            throw new ApiException(errorCode);
        }

        // 提交代付到PSP
        submitToPsp(entity);

        return toResponse(entity);
    }

    @Override
    public PayoutOrderResponse getByPayoutOrderNo(String payoutOrderNo) {
        PayoutOrderEntity entity = payoutOrderDao.selectOne(
                baseWrapper()
                        .eq("payout_order_no", StringUtils.trim(payoutOrderNo))
                        .last("limit 1")
        );
        return toResponse(entity);
    }

    @Override
    public PayoutOrderResponse getByMerchantOrderNo(String merchantOrderNo) {
        PayoutOrderEntity entity = payoutOrderDao.selectOne(
                baseWrapper()
                        .eq("merchant_order_no", StringUtils.trim(merchantOrderNo))
                        .last("limit 1")
        );
        return toResponse(entity);
    }

    private boolean insertOrder(PayoutOrderEntity entity) {
        try {
            payoutOrderDao.insert(entity);
            recordStatusChange(entity, null, entity.getStatus(), "ORDER_CREATED", null, "MERCHANT");
            return true;
        } catch (DuplicateKeyException ex) {
            PayoutOrderEntity existed = payoutOrderDao.selectOne(
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

    private void submitToPsp(PayoutOrderEntity order) {
        try {
            PspRouteResult route = pspRouteSelector.selectPayout(order);
            applyRoute(order, route);
            applyPspFee(order);

            PspPayoutDispatchResult dispatchResult = pspPayoutDispatchService.dispatch(order, route);
            applyDispatchResult(order, dispatchResult);
            if (!dispatchResult.isSuccess()) {
                releasePayout(order);
            }
            payoutOrderDao.updateById(order);
        } catch (ApiException ex) {
            releasePayout(order);
            markFailed(order, ex.getMessage(), ex.getErrorCode().name());
            throw ex;
        } catch (Exception ex) {
            releasePayout(order);
            markFailed(order, ApiErrorCode.SYSTEM_ERROR.getMessage(), ApiErrorCode.SYSTEM_ERROR.name());
            throw new ApiException(ApiErrorCode.SYSTEM_ERROR);
        }
    }

    private void applyRoute(PayoutOrderEntity entity, PspRouteResult route) {
        entity.setRouteRuleId(route.getRouteRuleId());
        entity.setRouteSnapshotJson(routeSnapshotJson(route));
        entity.setPspId(route.getPspId());
        entity.setPspCode(route.getPspCode());
        entity.setPspMethodId(route.getPspMethodId());
        entity.setPspMethodCode(route.getPspMethodCode());
        entity.setPspAccountId(route.getPspAccountId());
        entity.setPspAccountNo(route.getPspAccountNo());
    }

    private void applyDispatchResult(PayoutOrderEntity entity, PspPayoutDispatchResult result) {
        String fromStatus = entity.getStatus();
        entity.setPspRequestNo(result.getPspRequestNo());
        entity.setPspOrderNo(result.getPspOrderNo());
        entity.setPspRawStatus(result.getRawStatus());
        if (result.isSuccess()) {
            entity.setStatus(PayoutOrderStatusEnum.PROCESSING.code());
            entity.setPspStatus(PayoutOrderStatusEnum.PROCESSING.code());
            entity.setSubmittedAt(Instant.now());
            entity.setNextQueryAt(Instant.now().plusSeconds(60));
            recordStatusChange(entity, fromStatus, entity.getStatus(), "PSP_SUBMIT", null, "SYSTEM");
            return;
        }
        entity.setStatus(PayoutOrderStatusEnum.FAILED.code());
        entity.setPspStatus(PayoutOrderStatusEnum.FAILED.code());
        entity.setFailCode(result.getErrorCode());
        entity.setFailMsg(StringUtils.left(result.getErrorMessage(), 512));
        String reason = StringUtils.defaultIfBlank(
                result.getErrorMessage(),
                StringUtils.defaultIfBlank(result.getResponseMessage(), "PSP payout submit failed")
        );
        entity.setStatusReason(reason);
        entity.setFailedAt(Instant.now());
        recordStatusChange(entity, fromStatus, entity.getStatus(), "PSP_SUBMIT_FAILED", reason, "SYSTEM");
    }

    private void markFailed(PayoutOrderEntity entity, String reason, String failCode) {
        String fromStatus = entity.getStatus();
        entity.setStatus(PayoutOrderStatusEnum.FAILED.code());
        String message = StringUtils.defaultIfBlank(reason, "Payout order failed");
        entity.setStatusReason(message);
        entity.setFailCode(failCode);
        entity.setFailMsg(StringUtils.left(reason, 512));
        entity.setFailedAt(Instant.now());
        payoutOrderDao.updateById(entity);
        recordStatusChange(entity, fromStatus, entity.getStatus(), "ORDER_FAILED", message, "SYSTEM");
    }

    private void recordStatusChange(PayoutOrderEntity entity,
                                    String fromStatus,
                                    String toStatus,
                                    String eventType,
                                    String reason,
                                    String operatorType) {
        orderStatusLogService.recordChange(
                "PAYOUT",
                entity.getTenantId(),
                entity.getMerchantId(),
                entity.getId(),
                entity.getPayoutOrderNo(),
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

    private void applyMerchantFee(PayoutOrderEntity entity) {
        MerchantFeeResult feeResult = merchantFeeRuleService.calculatePayout(entity);
        BigDecimal feeAmount = defaultZero(feeResult.getMerchantFeeAmount());
        entity.setMerchantFeeAmount(feeAmount);
        entity.setTotalDebitAmount(entity.getAmount().add(feeAmount));
        entity.setMerchantFeeRuleId(feeResult.getRule().getId());
        entity.setMerchantFeeSnapshotJson(feeResult.getSnapshotJson());
    }

    /**
     * TODO 账务冻结核心
     * @param order 订单信息
     */
    private void freezePayout(PayoutOrderEntity order) {
        LedgerPostingResult result = ledgerPostingService.freezePayout(payoutPostingRequest(order));
        order.setHoldNo(result.getHoldNo());
        order.setFreezeJournalNo(result.getJournalNo());
        payoutOrderDao.updateById(order);
    }

    /**
     * TODO 账务冻结核心
     * @param order 订单信息
     */
    private void releasePayout(PayoutOrderEntity order) {
        if (StringUtils.isBlank(order.getHoldNo())) {
            return;
        }
        try {
            LedgerPostingResult result = ledgerPostingService.releasePayout(payoutPostingRequest(order));
            order.setReleaseJournalNo(result.getJournalNo());
        } catch (Exception ignored) {
            // Keep the original PSP error visible; ledger retry/release can be handled by operations.
        }
    }

    private PayoutPostingRequest payoutPostingRequest(PayoutOrderEntity entity) {
        PayoutPostingRequest request = new PayoutPostingRequest();
        request.setTenantId(entity.getTenantId());
        request.setMerchantId(entity.getMerchantId());
        request.setBizId(entity.getId());
        request.setPayoutOrderNo(entity.getPayoutOrderNo());
        request.setCurrency(entity.getCurrency());
        request.setAmount(entity.getAmount());
        request.setMerchantFeeAmount(entity.getMerchantFeeAmount());
        request.setTotalDebitAmount(entity.getTotalDebitAmount());
        return request;
    }

    private void applyPspFee(PayoutOrderEntity entity) {
        PspFeeResult feeResult = pspFeeRuleService.calculatePayout(entity);
        entity.setPspFeeAmount(feeResult.getPspFeeAmount());
        entity.setPspFeeRuleId(feeResult.getRule().getId());
        entity.setPspFeeSnapshotJson(feeResult.getSnapshotJson());
    }

    private void validateIdempotentRequest(PayoutOrderEntity existed, PayoutOrderCreateRequest request, String currency, String methodCode) {
        if (existed.getAmount() == null || request.getAmount() == null
                || existed.getAmount().compareTo(request.getAmount()) != 0
                || !StringUtils.equalsIgnoreCase(existed.getCurrency(), currency)
                || !StringUtils.equalsIgnoreCase(existed.getMethodCode(), methodCode)
                || !StringUtils.equals(StringUtils.trimToEmpty(existed.getNotifyUrl()), StringUtils.trimToEmpty(request.getNotifyUrl()))
                || !StringUtils.equals(StringUtils.trimToEmpty(existed.getPayeeAccountHash()), sha256Hex(request.getCustomerAccountNo()))) {
            throw new ApiException(ApiErrorCode.DUPLICATE_REQUEST, "merchant_order_id exists with different request parameters");
        }
    }

    private void validateIdempotentEntity(PayoutOrderEntity existed, PayoutOrderEntity entity) {
        if (existed.getAmount() == null || entity.getAmount() == null
                || existed.getAmount().compareTo(entity.getAmount()) != 0
                || !StringUtils.equalsIgnoreCase(existed.getCurrency(), entity.getCurrency())
                || !StringUtils.equalsIgnoreCase(existed.getMethodCode(), entity.getMethodCode())
                || !StringUtils.equals(StringUtils.trimToEmpty(existed.getNotifyUrl()), StringUtils.trimToEmpty(entity.getNotifyUrl()))
                || !StringUtils.equals(StringUtils.trimToEmpty(existed.getPayeeAccountHash()), StringUtils.trimToEmpty(entity.getPayeeAccountHash()))) {
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

    private void applyPayee(PayoutOrderEntity entity, PayoutOrderCreateRequest request) {
        PayoutOrderCreateRequest.Payee payee = request.getPayee();
        String payeeName = firstNotBlank(payee == null ? null : payee.getName(), request.getCustomerName());
        String accountNo = firstNotBlank(payee == null ? null : payee.getAccountNo(), request.getCustomerAccountNo());
        String bankCode = firstNotBlank(payee == null ? null : payee.getBankCode(), request.getCustomerAccountBankCci());
        String walletType = firstNotBlank(payee == null ? null : payee.getWalletType(), request.getCustomerAccountType());
        String phone = payee == null ? null : payee.getPhone();
        String email = payee == null ? null : payee.getEmail();

        entity.setPayeeName(payeeName);
        entity.setPayeeAccountMask(mask(accountNo, 4, 4));
        entity.setPayeeAccountHash(sha256Hex(accountNo));
        entity.setPayeeBankCode(bankCode);
        entity.setPayeeWalletType(walletType);
        entity.setPayeePhoneMask(mask(phone, 3, 4));
        entity.setPayeePhoneHash(sha256Hex(phone));
        entity.setPayeeEmailMask(maskEmail(email));
        entity.setPayeeEmailHash(sha256Hex(email));
        entity.setPayeeJson(toJson(payeeSnapshot(payeeName, accountNo, bankCode, walletType, phone, email)));
    }

    private Map<String, Object> payeeSnapshot(String name, String accountNo, String bankCode, String walletType, String phone, String email) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("name", name);
        snapshot.put("account_mask", mask(accountNo, 4, 4));
        snapshot.put("bank_code", bankCode);
        snapshot.put("wallet_type", walletType);
        snapshot.put("phone_mask", mask(phone, 3, 4));
        snapshot.put("email_mask", maskEmail(email));
        return snapshot;
    }

    private String routeSnapshotJson(PspRouteResult route) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("routeRuleId", route.getRouteRuleId());
        snapshot.put("pspId", route.getPspId());
        snapshot.put("pspCode", route.getPspCode());
        snapshot.put("pspMethodId", route.getPspMethodId());
        snapshot.put("pspMethodCode", route.getPspMethodCode());
        snapshot.put("pspAccountId", route.getPspAccountId());
        snapshot.put("pspAccountNo", route.getPspAccountNo());
        return toJson(snapshot);
    }

    private QueryWrapper<PayoutOrderEntity> baseWrapper() {
        return new QueryWrapper<PayoutOrderEntity>()
                .eq("tenant_id", ApiReqContextHolder.getTenantId())
                .eq("merchant_id", ApiReqContextHolder.getMerchantId());
    }

    private void copyOrder(PayoutOrderEntity source, PayoutOrderEntity target) {
        target.setId(source.getId());
        target.setTenantId(source.getTenantId());
        target.setMerchantId(source.getMerchantId());
        target.setMerchantNo(source.getMerchantNo());
        target.setMerchantAppId(source.getMerchantAppId());
        target.setAppId(source.getAppId());
        target.setPayoutOrderNo(source.getPayoutOrderNo());
        target.setMerchantOrderNo(source.getMerchantOrderNo());
        target.setStatus(source.getStatus());
        target.setStatusReason(source.getStatusReason());
        target.setAmount(source.getAmount());
        target.setMerchantFeeAmount(source.getMerchantFeeAmount());
        target.setMerchantFeeRuleId(source.getMerchantFeeRuleId());
        target.setMerchantFeeSnapshotJson(source.getMerchantFeeSnapshotJson());
        target.setTotalDebitAmount(source.getTotalDebitAmount());
        target.setPspFeeAmount(source.getPspFeeAmount());
        target.setPspFeeRuleId(source.getPspFeeRuleId());
        target.setPspFeeSnapshotJson(source.getPspFeeSnapshotJson());
        target.setCurrency(source.getCurrency());
        target.setCountryCode(source.getCountryCode());
        target.setMethodCode(source.getMethodCode());
        target.setPspOrderNo(source.getPspOrderNo());
    }

    private PayoutOrderResponse toResponse(PayoutOrderEntity entity) {
        if (entity == null) {
            throw new ApiException(ApiErrorCode.ORDER_NOT_FOUND);
        }
        PayoutOrderResponse response = new PayoutOrderResponse();
        response.setSystemOrderId(entity.getPayoutOrderNo());
        response.setMerchantOrderId(entity.getMerchantOrderNo());
        response.setStatus(entity.getStatus());
        response.setStatusReason(entity.getStatusReason());
        response.setAmount(formatMoney(entity.getAmount(), entity.getCurrency()));
        response.setCurrency(entity.getCurrency());
        response.setCountryCode(entity.getCountryCode());
        response.setMethodCode(entity.getMethodCode());
        return response;
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Map<?, ?> map && map.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new ApiException(ApiErrorCode.INVALID_REQUEST, "Invalid JSON field");
        }
    }

    private String formatMoney(BigDecimal value, String currency) {
        return value == null ? null : ApiAmountUtils.formatCurrencyAmount(value, currency);
    }

    private BigDecimal defaultZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String firstNotBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StringUtils.isNotBlank(value)) {
                return StringUtils.trim(value);
            }
        }
        return null;
    }

    private String mask(String value, int prefix, int suffix) {
        String text = StringUtils.trimToNull(value);
        if (text == null) {
            return null;
        }
        if (text.length() <= prefix + suffix) {
            return "*".repeat(Math.min(text.length(), 6));
        }
        return text.substring(0, prefix) + "****" + text.substring(text.length() - suffix);
    }

    private String maskEmail(String value) {
        String email = StringUtils.trimToNull(value);
        if (email == null) {
            return null;
        }
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) {
            return mask(email, 1, 0);
        }
        return email.charAt(0) + "****" + email.substring(atIndex);
    }

    private String sha256Hex(String value) {
        String text = StringUtils.trimToNull(value);
        if (text == null) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(text.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new ApiException(ApiErrorCode.SYSTEM_ERROR);
        }
    }
}
