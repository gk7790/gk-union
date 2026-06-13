package com.gk.psp.log;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gk.common.enums.BizTypeEnum;
import com.gk.common.utils.BizKeyUtils;
import com.gk.openapi.security.ApiReqContext;
import com.gk.openapi.security.ApiReqContextHolder;
import com.gk.payment.entity.PayOrderEntity;
import com.gk.payment.payout.entity.PayoutOrderEntity;
import com.gk.psp.dispatch.PspPayDispatchResult;
import com.gk.psp.dispatch.PspPayoutDispatchResult;
import com.gk.psp.entity.PspRequestLogEntity;
import com.gk.psp.query.PspOrderQueryResult;
import com.gk.psp.route.PspRouteResult;
import com.gk.psp.service.PspRequestLogService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executor;

@Slf4j
@Component
public class PspRequestLogger {
    private static final String DEFAULT_HTTP_METHOD = "POST";

    private final PspRequestLogService pspRequestLogService;
    private final ObjectMapper objectMapper;
    private final Executor pspRequestLogExecutor;

    public PspRequestLogger(
            PspRequestLogService pspRequestLogService,
            ObjectMapper objectMapper,
            @Qualifier("pspRequestLogExecutor") Executor pspRequestLogExecutor
    ) {
        this.pspRequestLogService = pspRequestLogService;
        this.objectMapper = objectMapper;
        this.pspRequestLogExecutor = pspRequestLogExecutor;
    }

    public void paySubmitSuccess(PayOrderEntity order, PspRouteResult route, PspPayDispatchResult result, long costMs) {
        PspRequestLogEntity entity = baseEntity(order, route, result);
        entity.setRequestUrl(StringUtils.defaultIfBlank(result.getRequestUrl(), defaultPayRequestUrl(route)));
        entity.setHttpMethod(StringUtils.defaultIfBlank(result.getHttpMethod(), DEFAULT_HTTP_METHOD));
        entity.setRequestHeadersJson(sanitizeText(result.getRequestHeadersJson()));
        entity.setRequestBody(sanitizeText(StringUtils.defaultIfBlank(result.getRequestBody(), toJson(payRequestSnapshot(order, route)))));
        entity.setResponseStatus(result.getResponseStatus());
        entity.setResponseBody(sanitizeText(result.getRawResponseJson()));
        entity.setSuccess(result.isSuccess() ? 1 : 0);
        entity.setErrorCode(result.getErrorCode());
        entity.setErrorMsg(StringUtils.left(result.getErrorMessage(), 1024));
        entity.setCostMs(costMs);
        submit(entity);
    }

    public void paySubmitFailed(PayOrderEntity order, PspRouteResult route, Throwable throwable, long costMs) {
        PspRequestLogEntity entity = baseEntity(order, route, null);
        entity.setRequestUrl(defaultPayRequestUrl(route));
        entity.setHttpMethod(DEFAULT_HTTP_METHOD);
        entity.setRequestBody(sanitizeText(toJson(payRequestSnapshot(order, route))));
        entity.setSuccess(0);
        entity.setErrorCode("PSP_SUBMIT_FAILED");
        entity.setErrorMsg(StringUtils.left(throwable == null ? null : throwable.getMessage(), 1024));
        entity.setCostMs(costMs);
        submit(entity);
    }

    public void payoutSubmitSuccess(PayoutOrderEntity order, PspRouteResult route, PspPayoutDispatchResult result, long costMs) {
        PspRequestLogEntity entity = baseEntity(order, route, result);
        entity.setRequestUrl(StringUtils.defaultIfBlank(result.getRequestUrl(), defaultPayoutRequestUrl(route)));
        entity.setHttpMethod(StringUtils.defaultIfBlank(result.getHttpMethod(), DEFAULT_HTTP_METHOD));
        entity.setRequestHeadersJson(sanitizeText(result.getRequestHeadersJson()));
        entity.setRequestBody(sanitizeText(StringUtils.defaultIfBlank(result.getRequestBody(), toJson(payoutRequestSnapshot(order, route)))));
        entity.setResponseStatus(result.getResponseStatus());
        entity.setResponseBody(sanitizeText(result.getRawResponseJson()));
        entity.setSuccess(result.isSuccess() ? 1 : 0);
        entity.setErrorCode(result.getErrorCode());
        entity.setErrorMsg(StringUtils.left(result.getErrorMessage(), 1024));
        entity.setCostMs(costMs);
        submit(entity);
    }

    public void payoutSubmitFailed(PayoutOrderEntity order, PspRouteResult route, Throwable throwable, long costMs) {
        PspRequestLogEntity entity = baseEntity(order, route, null);
        entity.setRequestUrl(defaultPayoutRequestUrl(route));
        entity.setHttpMethod(DEFAULT_HTTP_METHOD);
        entity.setRequestBody(sanitizeText(toJson(payoutRequestSnapshot(order, route))));
        entity.setSuccess(0);
        entity.setErrorCode("PSP_PAYOUT_SUBMIT_FAILED");
        entity.setErrorMsg(StringUtils.left(throwable == null ? null : throwable.getMessage(), 1024));
        entity.setCostMs(costMs);
        submit(entity);
    }

    public void payQuerySuccess(PayOrderEntity order, PspRouteResult route, PspOrderQueryResult result, long costMs) {
        PspRequestLogEntity entity = baseEntity(order, route, null);
        applyQueryResult(entity, result, defaultPayQueryUrl(route), costMs);
        submit(entity);
    }

    public void payQueryFailed(PayOrderEntity order, PspRouteResult route, Throwable throwable, long costMs) {
        PspRequestLogEntity entity = baseEntity(order, route, null);
        entity.setRequestUrl(defaultPayQueryUrl(route));
        entity.setHttpMethod(DEFAULT_HTTP_METHOD);
        entity.setRequestBody(sanitizeText(toJson(payRequestSnapshot(order, route))));
        entity.setSuccess(0);
        entity.setErrorCode("PSP_PAY_QUERY_FAILED");
        entity.setErrorMsg(StringUtils.left(throwable == null ? null : throwable.getMessage(), 1024));
        entity.setCostMs(costMs);
        submit(entity);
    }

    public void payoutQuerySuccess(PayoutOrderEntity order, PspRouteResult route, PspOrderQueryResult result, long costMs) {
        PspRequestLogEntity entity = baseEntity(order, route, null);
        applyQueryResult(entity, result, defaultPayoutQueryUrl(route), costMs);
        submit(entity);
    }

    public void payoutQueryFailed(PayoutOrderEntity order, PspRouteResult route, Throwable throwable, long costMs) {
        PspRequestLogEntity entity = baseEntity(order, route, null);
        entity.setRequestUrl(defaultPayoutQueryUrl(route));
        entity.setHttpMethod(DEFAULT_HTTP_METHOD);
        entity.setRequestBody(sanitizeText(toJson(payoutRequestSnapshot(order, route))));
        entity.setSuccess(0);
        entity.setErrorCode("PSP_PAYOUT_QUERY_FAILED");
        entity.setErrorMsg(StringUtils.left(throwable == null ? null : throwable.getMessage(), 1024));
        entity.setCostMs(costMs);
        submit(entity);
    }

    private PspRequestLogEntity baseEntity(PayOrderEntity order, PspRouteResult route, PspPayDispatchResult result) {
        PspRequestLogEntity entity = new PspRequestLogEntity();
        entity.setTenantId(order == null ? null : order.getTenantId());
        entity.setMerchantId(order == null ? null : order.getMerchantId());
        entity.setPspId(route == null ? null : route.getPspId());
        entity.setPspCode(route == null ? null : route.getPspCode());
        entity.setBizType(BizTypeEnum.PAY_ORDER.code());
        entity.setBizId(order == null ? null : order.getId());
        entity.setBizNo(order == null ? null : order.getPayOrderNo());
        String pspRequestNo = result == null ? null : result.getPspRequestNo();
        entity.setRequestNo(StringUtils.defaultIfBlank(pspRequestNo, BizKeyUtils.genPspRequestNo()));
        entity.setPspRequestNo(pspRequestNo);
        entity.setPspOrderNo(result == null ? null : result.getPspOrderNo());
        ApiReqContext context = ApiReqContextHolder.get();
        entity.setTraceId(context == null ? null : context.getTraceId());
        return entity;
    }

    private PspRequestLogEntity baseEntity(PayoutOrderEntity order, PspRouteResult route, PspPayoutDispatchResult result) {
        PspRequestLogEntity entity = new PspRequestLogEntity();
        entity.setTenantId(order == null ? null : order.getTenantId());
        entity.setMerchantId(order == null ? null : order.getMerchantId());
        entity.setPspId(route == null ? null : route.getPspId());
        entity.setPspCode(route == null ? null : route.getPspCode());
        entity.setBizType(BizTypeEnum.PAYOUT_ORDER.code());
        entity.setBizId(order == null ? null : order.getId());
        entity.setBizNo(order == null ? null : order.getPayoutOrderNo());
        String pspRequestNo = result == null ? null : result.getPspRequestNo();
        entity.setRequestNo(StringUtils.defaultIfBlank(pspRequestNo, BizKeyUtils.genPspRequestNo()));
        entity.setPspRequestNo(pspRequestNo);
        entity.setPspOrderNo(result == null ? null : result.getPspOrderNo());
        ApiReqContext context = ApiReqContextHolder.get();
        entity.setTraceId(context == null ? null : context.getTraceId());
        return entity;
    }

    private Map<String, Object> payRequestSnapshot(PayOrderEntity order, PspRouteResult route) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("pay_order_no", order == null ? null : order.getPayOrderNo());
        snapshot.put("merchant_order_no", order == null ? null : order.getMerchantOrderNo());
        snapshot.put("amount", order == null ? null : order.getAmount());
        snapshot.put("currency", order == null ? null : order.getCurrency());
        snapshot.put("method_code", order == null ? null : order.getMethodCode());
        snapshot.put("psp_method_code", route == null ? null : route.getPspMethodCode());
        snapshot.put("psp_account_no", route == null ? null : route.getPspAccountNo());
        return snapshot;
    }

    private Map<String, Object> payoutRequestSnapshot(PayoutOrderEntity order, PspRouteResult route) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("payout_order_no", order == null ? null : order.getPayoutOrderNo());
        snapshot.put("merchant_order_no", order == null ? null : order.getMerchantOrderNo());
        snapshot.put("amount", order == null ? null : order.getAmount());
        snapshot.put("currency", order == null ? null : order.getCurrency());
        snapshot.put("method_code", order == null ? null : order.getMethodCode());
        snapshot.put("psp_method_code", route == null ? null : route.getPspMethodCode());
        snapshot.put("psp_account_no", route == null ? null : route.getPspAccountNo());
        snapshot.put("payee_name", order == null ? null : order.getPayeeName());
        snapshot.put("payee_account_mask", order == null ? null : order.getPayeeAccountMask());
        snapshot.put("payee_wallet_type", order == null ? null : order.getPayeeWalletType());
        return snapshot;
    }

    private String defaultPayRequestUrl(PspRouteResult route) {
        if (route == null || StringUtils.isBlank(route.getPspBaseUrl())) {
            return null;
        }
        return StringUtils.removeEnd(route.getPspBaseUrl(), "/") + "/open-api/create-pay-order";
    }

    private String defaultPayoutRequestUrl(PspRouteResult route) {
        if (route == null || StringUtils.isBlank(route.getPspBaseUrl())) {
            return null;
        }
        return StringUtils.removeEnd(route.getPspBaseUrl(), "/") + "/open-api/create-payout-order";
    }

    private void applyQueryResult(PspRequestLogEntity entity, PspOrderQueryResult result, String defaultUrl, long costMs) {
        entity.setRequestNo(StringUtils.defaultIfBlank(result.getPspRequestNo(), entity.getRequestNo()));
        entity.setPspRequestNo(result.getPspRequestNo());
        entity.setPspOrderNo(result.getPspOrderNo());
        entity.setRequestUrl(StringUtils.defaultIfBlank(result.getRequestUrl(), defaultUrl));
        entity.setHttpMethod(StringUtils.defaultIfBlank(result.getHttpMethod(), DEFAULT_HTTP_METHOD));
        entity.setRequestHeadersJson(sanitizeText(result.getRequestHeadersJson()));
        entity.setRequestBody(sanitizeText(result.getRequestBody()));
        entity.setResponseStatus(result.getResponseStatus());
        entity.setResponseBody(sanitizeText(result.getRawResponseJson()));
        entity.setSuccess(result.isSuccess() ? 1 : 0);
        entity.setErrorCode(result.getErrorCode());
        entity.setErrorMsg(StringUtils.left(result.getErrorMessage(), 1024));
        entity.setCostMs(costMs);
    }

    private String defaultPayQueryUrl(PspRouteResult route) {
        if (route == null || StringUtils.isBlank(route.getPspBaseUrl())) {
            return null;
        }
        return StringUtils.removeEnd(route.getPspBaseUrl(), "/") + "/open-api/query-pay-order";
    }

    private String defaultPayoutQueryUrl(PspRouteResult route) {
        if (route == null || StringUtils.isBlank(route.getPspBaseUrl())) {
            return null;
        }
        return StringUtils.removeEnd(route.getPspBaseUrl(), "/") + "/open-api/query-payout-order";
    }

    private void submit(PspRequestLogEntity entity) {
        try {
            pspRequestLogExecutor.execute(() -> pspRequestLogService.record(entity));
        } catch (Exception ex) {
            log.warn("Submit PSP request log failed: {}", ex.getMessage());
        }
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            return null;
        }
    }

    private String sanitizeText(String value) {
        if (StringUtils.isBlank(value)) {
            return value;
        }
        return value
                .replaceAll("(?i)(\"(?:api_secret|secret|password|token|access_token|sign|signature|api_key)\"\\s*:\\s*\")[^\"]*(\")", "$1***$2")
                .replaceAll("(?i)((?:api_secret|secret|password|token|access_token|sign|signature|api_key)=)[^&\\s]*", "$1***");
    }
}
