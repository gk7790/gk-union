package com.gk.payment.plan;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gk.infra.enums.StatusEnum;
import com.gk.merchant.dao.MerchantAppDao;
import com.gk.merchant.dao.MerchantDao;
import com.gk.merchant.entity.MerchantAppEntity;
import com.gk.merchant.entity.MerchantEntity;
import com.gk.openapi.error.ApiErrorCode;
import com.gk.openapi.error.ApiException;
import com.gk.payment.dto.PayinConfigPrecheckRequest;
import com.gk.payment.dto.PayinConfigPrecheckResult;
import com.gk.payment.entity.PayOrderEntity;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class PayinPlanServiceImpl implements PayinPlanService {
    private final MerchantDao merchantDao;
    private final MerchantAppDao merchantAppDao;
    private final ObjectMapper objectMapper;
    private final PaymentPlanResolver paymentPlanResolver;
    private final PaymentPlanCacheService paymentPlanCacheService;

    @Override
    public PayinPlan resolve(PayOrderEntity order) {
        PaymentPlan paymentPlan = paymentPlanResolver.resolvePayin(order)
                .orElseThrow(() -> new ApiException(ApiErrorCode.UNSUPPORTED_METHOD, "ACTIVE payment plan is not published"));
        return toPayinPlan(paymentPlan);
    }

    @Override
    public PayinConfigPrecheckResult precheck(PayinConfigPrecheckRequest request) {
        PayinConfigPrecheckResult result = new PayinConfigPrecheckResult();
        if (!validateRequest(request, result)) {
            result.refreshPassed();
            return result;
        }

        MerchantEntity merchant = merchantDao.selectById(request.getMerchantId());
        MerchantAppEntity app = merchantAppDao.selectById(request.getMerchantAppId());
        validateMerchant(request, merchant, result);
        validateApp(request, app, result);
        if (!result.getErrors().isEmpty()) {
            result.refreshPassed();
            return result;
        }

        PayOrderEntity order = toOrder(request, merchant, app);
        try {
            if (paymentPlanResolver.resolvePayin(order).isEmpty()) {
                result.addError("PAYMENT_PLAN_NOT_PUBLISHED", "ACTIVE payment plan is not published");
            }
        } catch (ApiException ex) {
            result.addError(toConfigErrorCode(ex), ex.getMessage());
        } catch (Exception ex) {
            result.addError("PAYIN_PLAN_INVALID", ex.getMessage());
        }
        result.refreshPassed();
        return result;
    }

    @Override
    public void evictAll() {
        paymentPlanCacheService.evictAll();
    }

    private boolean validateRequest(PayinConfigPrecheckRequest request, PayinConfigPrecheckResult result) {
        if (request == null) {
            result.addError("REQUEST_EMPTY", "request is required");
            return false;
        }
        if (request.getTenantId() == null) {
            result.addError("TENANT_ID_REQUIRED", "tenantId is required");
        }
        if (request.getMerchantId() == null) {
            result.addError("MERCHANT_ID_REQUIRED", "merchantId is required");
        }
        if (request.getMerchantAppId() == null) {
            result.addError("MERCHANT_APP_ID_REQUIRED", "merchantAppId is required");
        }
        if (StringUtils.isBlank(request.getCurrency())) {
            result.addError("CURRENCY_REQUIRED", "currency is required");
        }
        if (StringUtils.isBlank(request.getMethodCode())) {
            result.addError("METHOD_CODE_REQUIRED", "methodCode is required");
        }
        if (request.getAmount() == null || request.getAmount().signum() <= 0) {
            result.addError("AMOUNT_INVALID", "amount must be greater than 0");
        }
        return result.getErrors().isEmpty();
    }

    private void validateMerchant(PayinConfigPrecheckRequest request, MerchantEntity merchant, PayinConfigPrecheckResult result) {
        if (merchant == null || !request.getTenantId().equals(merchant.getTenantId())) {
            result.addError("MERCHANT_NOT_FOUND", "merchant is not found");
            return;
        }
        if (!StatusEnum.NORMAL.code().equals(merchant.getStatus())) {
            result.addError("MERCHANT_DISABLED", "merchant is disabled");
        }
    }

    private void validateApp(PayinConfigPrecheckRequest request, MerchantAppEntity app, PayinConfigPrecheckResult result) {
        if (app == null
                || !request.getTenantId().equals(app.getTenantId())
                || !request.getMerchantId().equals(app.getMerchantId())) {
            result.addError("MERCHANT_APP_NOT_FOUND", "merchant app is not found");
            return;
        }
        if (!StatusEnum.NORMAL.code().equals(app.getStatus())) {
            result.addError("MERCHANT_APP_DISABLED", "merchant app is disabled");
        }
        if (notAllowed(app.getAllowedCurrencyJson(), request.getCurrency())) {
            result.addError("CURRENCY_NOT_ALLOWED", "currency is not allowed for app");
        }
        if (notAllowed(app.getAllowedMethodJson(), request.getMethodCode())) {
            result.addError("METHOD_NOT_ALLOWED", "method is not allowed for app");
        }
    }

    private PayOrderEntity toOrder(PayinConfigPrecheckRequest request, MerchantEntity merchant, MerchantAppEntity app) {
        PayOrderEntity order = new PayOrderEntity();
        order.setTenantId(request.getTenantId());
        order.setMerchantId(request.getMerchantId());
        order.setMerchantNo(merchant.getMerchantNo());
        order.setMerchantAppId(request.getMerchantAppId());
        order.setAppId(app.getAppId());
        order.setCountryCode(normalize(request.getCountryCode()));
        order.setCurrency(normalize(request.getCurrency()));
        order.setMethodCode(normalize(request.getMethodCode()));
        order.setAmount(request.getAmount());
        return order;
    }

    private String toConfigErrorCode(ApiException ex) {
        String message = StringUtils.defaultString(ex.getMessage());
        if (StringUtils.containsIgnoreCase(message, "payment plan")) {
            return "PAYMENT_PLAN_NOT_PUBLISHED";
        }
        if (StringUtils.containsIgnoreCase(message, "Merchant fee rule")) {
            return "MERCHANT_FEE_RULE_MISSING";
        }
        if (StringUtils.containsIgnoreCase(message, "PSP provider")) {
            return "PSP_PROVIDER_UNAVAILABLE";
        }
        if (StringUtils.containsIgnoreCase(message, "PSP method")) {
            return "PSP_METHOD_UNAVAILABLE";
        }
        if (StringUtils.containsIgnoreCase(message, "PSP account")) {
            return "PSP_ACCOUNT_UNAVAILABLE";
        }
        if (StringUtils.containsIgnoreCase(message, "PSP fee rule")) {
            return "PSP_FEE_RULE_MISSING";
        }
        ApiErrorCode errorCode = ex.getErrorCode();
        return errorCode == null ? "PAYIN_PLAN_INVALID" : errorCode.name();
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

    private boolean notAllowed(String jsonArray, String value) {
        return !allowed(jsonArray, value);
    }

    private String normalize(String value) {
        return StringUtils.defaultString(value).trim().toUpperCase(Locale.ROOT);
    }

    private PayinPlan toPayinPlan(PaymentPlan paymentPlan) {
        PayinPlan plan = new PayinPlan();
        plan.setCatalogId(paymentPlan.getCatalogId());
        plan.setCatalogVersion(paymentPlan.getCatalogVersion());
        plan.setBucketId(paymentPlan.getBucketId());
        plan.setRouteOptionId(paymentPlan.getRouteOptionId());
        plan.setMerchantFee(paymentPlan.getMerchantFee());
        plan.setRoute(paymentPlan.getRoute());
        plan.setPspFee(paymentPlan.getPspFee());
        plan.setMerchantFeeAmount(paymentPlan.getMerchantFeeAmount());
        plan.setSettleAmount(paymentPlan.getSettleAmount());
        plan.setPspFeeAmount(paymentPlan.getPspFeeAmount());
        return plan;
    }
}
