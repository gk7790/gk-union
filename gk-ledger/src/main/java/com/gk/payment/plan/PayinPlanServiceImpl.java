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
import com.gk.payment.fee.MerchantFeeResult;
import com.gk.payment.service.MerchantFeeRuleService;
import com.gk.psp.fee.PspFeeResult;
import com.gk.psp.route.PspRouteResult;
import com.gk.psp.route.PspRouteSelector;
import com.gk.psp.service.PspFeeRuleService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class PayinPlanServiceImpl implements PayinPlanService {
    private final MerchantDao merchantDao;
    private final MerchantAppDao merchantAppDao;
    private final MerchantFeeRuleService merchantFeeRuleService;
    private final PspRouteSelector pspRouteSelector;
    private final PspFeeRuleService pspFeeRuleService;
    private final ObjectMapper objectMapper;
    private final PaymentPlanResolver paymentPlanResolver;
    private final PaymentPlanCacheService paymentPlanCacheService;

    /**
     * 代收下单主链路使用的方案解析入口。
     * <p>
     * 这里把商户费率、PSP 路由、PSP 成本费率一次性解析出来，避免下单流程分散查询多张配置表。
     */
    @Override
    public PayinPlan resolve(PayOrderEntity order) {
        // 优先读取后台已经发布的支付决策表；没有 ACTIVE 版本时，平滑降级到旧的实时解析逻辑。
        PaymentPlan compiledPlan = paymentPlanResolver.resolvePayin(order).orElse(null);
        if (compiledPlan != null) {
            return toPayinPlan(compiledPlan);
        }
        // 下单链路要求 PSP 成本费率完整；配置不完整时直接失败，避免生成不可核算的订单。
        return buildPlan(order, false);
    }

    /**
     * 后台发布配置前的预检入口。
     * <p>
     * 预检复用真实下单解析逻辑，但 PSP 成本费率缺失只返回 warning，方便后台先发现问题再决定是否补齐。
     */
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
            buildPlan(order, false, result);
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
        // 同时清理旧本地缓存和新的 Redis 决策表缓存，兼容迁移期间两套解析路径。
        paymentPlanCacheService.evictAll();
    }

    private PayinPlan buildPlan(PayOrderEntity order, boolean requirePspFee) {
        return buildPlan(order, requirePspFee, null);
    }

    private PayinPlan buildPlan(PayOrderEntity order, boolean requirePspFee, PayinConfigPrecheckResult precheckResult) {
        PayOrderEntity working = copyOrder(order);
        // 1. 商户费率决定商户手续费和商户视角结算金额。
        MerchantFeeResult merchantFee = merchantFeeRuleService.calculatePayin(working);
        // 2. 路由决定本次订单走哪个 PSP、哪个 PSP method、哪个 PSP account。
        PspRouteResult route = pspRouteSelector.selectPayin(working);
        applyRoute(working, route);

        // 3. PSP 成本费率依赖路由结果，所以必须在 applyRoute 后再计算。
        PspFeeResult pspFee = null;
        try {
            pspFee = pspFeeRuleService.calculatePayin(working);
        } catch (ApiException ex) {
            if (requirePspFee) {
                throw ex;
            }
            if (precheckResult != null) {
                precheckResult.addWarning(toConfigErrorCode(ex), ex.getMessage());
            }
        }
        if (pspFee == null && requirePspFee) {
            throw new ApiException(ApiErrorCode.INVALID_REQUEST, "PSP fee rule is not configured");
        }
        if (pspFee == null && !requirePspFee && precheckResult != null) {
            precheckResult.addWarning("PSP_FEE_RULE_MISSING", "PSP fee rule is not configured");
        }

        PayinPlan plan = new PayinPlan();
        plan.setMerchantFee(merchantFee);
        plan.setRoute(route);
        plan.setPspFee(pspFee);
        plan.setMerchantFeeAmount(merchantFee.getMerchantFeeAmount());
        plan.setSettleAmount(merchantFee.getSettleAmount());
        plan.setPspFeeAmount(pspFee == null ? BigDecimal.ZERO : pspFee.getPspFeeAmount());
        return plan;
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
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
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
        if (!allowed(app.getAllowedCurrencyJson(), request.getCurrency())) {
            result.addError("CURRENCY_NOT_ALLOWED", "currency is not allowed for app");
        }
        if (!allowed(app.getAllowedMethodJson(), request.getMethodCode())) {
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

    private PayOrderEntity copyOrder(PayOrderEntity source) {
        PayOrderEntity target = new PayOrderEntity();
        target.setTenantId(source.getTenantId());
        target.setMerchantId(source.getMerchantId());
        target.setMerchantNo(source.getMerchantNo());
        target.setMerchantAppId(source.getMerchantAppId());
        target.setAppId(source.getAppId());
        target.setCountryCode(normalize(source.getCountryCode()));
        target.setCurrency(normalize(source.getCurrency()));
        target.setMethodCode(normalize(source.getMethodCode()));
        target.setAmount(source.getAmount());
        return target;
    }

    private void applyRoute(PayOrderEntity order, PspRouteResult route) {
        // 把路由结果写回工作订单，后续 PSP 成本费率和订单快照都使用同一套路由口径。
        order.setRouteRuleId(route.getRouteRuleId());
        order.setPspId(route.getPspId());
        order.setPspCode(route.getPspCode());
        order.setPspMethodId(route.getPspMethodId());
        order.setPspMethodCode(route.getPspMethodCode());
        order.setPspAccountId(route.getPspAccountId());
        order.setPspAccountNo(route.getPspAccountNo());
    }

    private String toConfigErrorCode(ApiException ex) {
        String message = StringUtils.defaultString(ex.getMessage());
        if (StringUtils.containsIgnoreCase(message, "Merchant fee rule")) {
            return "MERCHANT_FEE_RULE_MISSING";
        }
        if (StringUtils.containsIgnoreCase(message, "No available PSP route")) {
            return "PSP_ROUTE_RULE_MISSING";
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

    private String normalize(String value) {
        return StringUtils.defaultString(value).trim().toUpperCase(Locale.ROOT);
    }

    private PayinPlan toPayinPlan(PaymentPlan paymentPlan) {
        PayinPlan plan = new PayinPlan();
        plan.setMerchantFee(paymentPlan.getMerchantFee());
        plan.setRoute(paymentPlan.getRoute());
        plan.setPspFee(paymentPlan.getPspFee());
        plan.setMerchantFeeAmount(paymentPlan.getMerchantFeeAmount());
        plan.setSettleAmount(paymentPlan.getSettleAmount());
        plan.setPspFeeAmount(paymentPlan.getPspFeeAmount());
        return plan;
    }
}
