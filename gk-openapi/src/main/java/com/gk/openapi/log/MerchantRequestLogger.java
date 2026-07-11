package com.gk.openapi.log;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gk.payment.domain.enums.BizTypeEnum;
import com.gk.payment.domain.key.BizKeyUtils;
import com.gk.openapi.dto.BalanceQueryRequest;
import com.gk.openapi.dto.BalanceResponse;
import com.gk.openapi.dto.PayinOrderCreateRequest;
import com.gk.openapi.dto.PayinOrderQueryRequest;
import com.gk.openapi.dto.PayoutOrderCreateRequest;
import com.gk.openapi.dto.PayoutOrderQueryRequest;
import com.gk.openapi.error.ApiErrorDescriptor;
import com.gk.openapi.error.ApiExceptionMapper;
import com.gk.openapi.security.ApiReqContext;
import com.gk.openapi.security.ApiReqContextHolder;
import com.gk.openapi.security.OpenApiAuthFilter;
import com.gk.openapi.tools.ApiR;
import com.gk.payment.entity.MerchantRequestLogEntity;
import com.gk.payment.merchantview.PayinOrderView;
import com.gk.payment.merchantview.PayoutOrderView;
import com.gk.payment.service.MerchantRequestLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

@Slf4j
@Component
public class MerchantRequestLogger {
    private final MerchantRequestLogService merchantRequestLogService;
    private final ObjectMapper objectMapper;
    private final Executor merchantRequestLogExecutor;

    public MerchantRequestLogger(
            MerchantRequestLogService merchantRequestLogService,
            ObjectMapper objectMapper,
            @Qualifier("merchantRequestLogExecutor") Executor merchantRequestLogExecutor
    ) {
        this.merchantRequestLogService = merchantRequestLogService;
        this.objectMapper = objectMapper;
        this.merchantRequestLogExecutor = merchantRequestLogExecutor;
    }

    public void balanceSuccess(HttpServletRequest request, BalanceQueryRequest body, List<BalanceResponse> data, long startMs) {
        record(request, "查询余额", "BALANCE", null, null, balanceResponseSummary(body, data), null, startMs);
    }

    public void balanceFailed(HttpServletRequest request, Throwable throwable, long startMs) {
        record(request, "查询余额", "BALANCE", null, null, null, throwable, startMs);
    }

    public void payCreateSuccess(HttpServletRequest request, PayinOrderCreateRequest body, PayinOrderView orderResp, ApiR<PayinOrderView> response, long startMs) {
        record(
                request,
                "创建代收订单",
                BizTypeEnum.PAYIN_ORDER.code(),
                body == null ? null : body.getMerchantOrderId(),
                orderResp == null ? null : orderResp.getSystemOrderId(),
                response,
                null,
                startMs
        );
    }

    public void payCreateFailed(HttpServletRequest request, PayinOrderCreateRequest body, Throwable throwable, long startMs) {
        record(
                request,
                "创建代收订单",
                BizTypeEnum.PAYIN_ORDER.code(),
                body == null ? getSignParam(request, "merchant_order_id") : body.getMerchantOrderId(),
                null,
                null,
                throwable,
                startMs
        );
    }

    public void payQuerySuccess(HttpServletRequest request, PayinOrderQueryRequest body, PayinOrderView orderResp, ApiR<PayinOrderView> response, long startMs) {
        record(
                request,
                "查询代收订单",
                BizTypeEnum.PAYIN_ORDER.code(),
                firstNotBlank(body == null ? null : body.getMerchantOrderId(), orderResp == null ? null : orderResp.getMerchantOrderId()),
                firstNotBlank(orderResp == null ? null : orderResp.getSystemOrderId(), body == null ? null : body.getSystemOrderId()),
                response,
                null,
                startMs
        );
    }

    public void payQueryFailed(HttpServletRequest request, PayinOrderQueryRequest body, Throwable throwable, long startMs) {
        record(
                request,
                "查询代收订单",
                BizTypeEnum.PAYIN_ORDER.code(),
                firstNotBlank(body == null ? null : body.getMerchantOrderId(), getSignParam(request, "merchant_order_id")),
                firstNotBlank(body == null ? null : body.getSystemOrderId(), getSignParam(request, "system_order_id")),
                null,
                throwable,
                startMs
        );
    }

    public void payoutCreateSuccess(HttpServletRequest request, PayoutOrderCreateRequest body, PayoutOrderView orderResp, ApiR<PayoutOrderView> response, long startMs) {
        record(
                request,
                "创建代付订单",
                BizTypeEnum.PAYOUT_ORDER.code(),
                body == null ? null : body.getMerchantOrderId(),
                orderResp == null ? null : orderResp.getSystemOrderId(),
                response,
                null,
                startMs
        );
    }

    public void payoutCreateFailed(HttpServletRequest request, PayoutOrderCreateRequest body, Throwable throwable, long startMs) {
        record(
                request,
                "创建代付订单",
                BizTypeEnum.PAYOUT_ORDER.code(),
                body == null ? getSignParam(request, "merchant_order_id") : body.getMerchantOrderId(),
                null,
                null,
                throwable,
                startMs
        );
    }

    public void payoutQuerySuccess(HttpServletRequest request, PayoutOrderQueryRequest body, PayoutOrderView orderResp, ApiR<PayoutOrderView> response, long startMs) {
        record(
                request,
                "查询代付订单",
                BizTypeEnum.PAYOUT_ORDER.code(),
                firstNotBlank(body == null ? null : body.getMerchantOrderId(), orderResp == null ? null : orderResp.getMerchantOrderId()),
                firstNotBlank(orderResp == null ? null : orderResp.getSystemOrderId(), body == null ? null : body.getSystemOrderId()),
                response,
                null,
                startMs
        );
    }

    public void payoutQueryFailed(HttpServletRequest request, PayoutOrderQueryRequest body, Throwable throwable, long startMs) {
        record(
                request,
                "查询代付订单",
                BizTypeEnum.PAYOUT_ORDER.code(),
                firstNotBlank(body == null ? null : body.getMerchantOrderId(), getSignParam(request, "merchant_order_id")),
                firstNotBlank(body == null ? null : body.getSystemOrderId(), getSignParam(request, "system_order_id")),
                null,
                throwable,
                startMs
        );
    }

    public void authRejected(HttpServletRequest request, String traceId, String rawBody, String code, String message) {
        Map<String, Object> params = getSignParams(request);
        MerchantRequestLogEntity entity = baseEntity(request, params, null);
        entity.setAppId(getParam(params, OpenApiAuthFilter.PARAM_APP_ID));
        entity.setApiName("OpenAPI鉴权");
        entity.setClientIp(getClientIp(request));
        entity.setRequestBodyHash(MerchantRequestLogUtils.sha256Hex(rawBody));
        entity.setRequestBodyJson(toJson(MerchantRequestLogUtils.sanitizeParams(params)));
        entity.setRequestParamsJson(entity.getRequestBodyJson());
        entity.setSignType(getParam(params, OpenApiAuthFilter.PARAM_SIGN_TYPE));
        entity.setSignValue(MerchantRequestLogUtils.maskSignature(getParam(params, OpenApiAuthFilter.PARAM_SIGN)));
        entity.setSignValid(0);
        entity.setTimestampValue(getParam(params, OpenApiAuthFilter.PARAM_TIMESTAMP));
        entity.setNonceValue(getParam(params, OpenApiAuthFilter.PARAM_NONCE));
        entity.setResponseCode(code);
        entity.setResponseMessage(StringUtils.left(message, 512));
        entity.setResponseBodyJson(toJson(ApiR.error(code, message)));
        entity.setStatus("REJECTED");
        entity.setErrorCode(code);
        entity.setErrorMessage(StringUtils.left(message, 512));
        entity.setTraceId(traceId);
        submit(entity);
    }

    private void record(
            HttpServletRequest request,
            String apiName,
            String bizType,
            String merchantOrderNo,
            String bizNo,
            Object responseBody,
            Throwable throwable,
            long startMs
    ) {
        Map<String, Object> params = getSignParams(request);
        ApiReqContext context = ApiReqContextHolder.get();
        MerchantRequestLogEntity entity = baseEntity(request, params, context);
        entity.setApiName(apiName);
        String rawRequestJson = toJson(params);
        entity.setRequestBodyHash(MerchantRequestLogUtils.sha256Hex(rawRequestJson));
        entity.setRequestBodyJson(toJson(MerchantRequestLogUtils.sanitizeParams(params)));
        entity.setRequestParamsJson(entity.getRequestBodyJson());
        entity.setSignType(getParam(params, OpenApiAuthFilter.PARAM_SIGN_TYPE));
        entity.setSignValue(MerchantRequestLogUtils.maskSignature(getParam(params, OpenApiAuthFilter.PARAM_SIGN)));
        entity.setSignValid(context == null ? null : 1);
        entity.setTimestampValue(getParam(params, OpenApiAuthFilter.PARAM_TIMESTAMP));
        entity.setNonceValue(getParam(params, OpenApiAuthFilter.PARAM_NONCE));
        entity.setBizType(bizType);
        entity.setBizNo(bizNo);
        entity.setMerchantOrderNo(merchantOrderNo);
        entity.setCostMs(Math.max(0, System.currentTimeMillis() - startMs));
        entity.setTraceId(context == null ? null : context.getTraceId());

        if (throwable == null) {
            entity.setStatus("SUCCESS");
            entity.setResponseCode("SUCCESS");
            entity.setResponseMessage("success");
            entity.setResponseBodyJson(toJson(responseBody));
        } else {
            entity.setStatus("FAILED");
            ApiErrorDescriptor descriptor = ApiExceptionMapper.resolve(throwable);
            entity.setResponseCode(descriptor.code().name());
            entity.setErrorCode(descriptor.code().name());
            entity.setResponseMessage(StringUtils.left(descriptor.publicMessage(), 512));
            entity.setErrorMessage(StringUtils.left(descriptor.publicMessage(), 512));
            entity.setResponseBodyJson(toJson(ApiR.error(entity.getResponseCode(), entity.getResponseMessage())));
        }
        submit(entity);
    }

    private MerchantRequestLogEntity baseEntity(HttpServletRequest request, Map<String, Object> params, ApiReqContext context) {
        MerchantRequestLogEntity entity = new MerchantRequestLogEntity();
        entity.setTenantId(context == null ? null : context.getTenantId());
        entity.setMerchantId(context == null ? null : context.getMerchantId());
        entity.setMerchantNo(context == null ? null : context.getMerchantNo());
        entity.setMerchantAppId(context == null ? null : context.getMerchantAppId());
        entity.setAppId(context == null ? getParam(params, OpenApiAuthFilter.PARAM_APP_ID) : context.getAppId());
        entity.setRequestNo(BizKeyUtils.genMerchantRequestNo());
        entity.setApiPath(request.getRequestURI());
        entity.setHttpMethod(request.getMethod());
        entity.setClientIp(context == null ? getClientIp(request) : context.getClientIp());
        entity.setUserAgent(StringUtils.left(request.getHeader("User-Agent"), 512));
        return entity;
    }

    private void submit(MerchantRequestLogEntity entity) {
        try {
            merchantRequestLogExecutor.execute(() -> merchantRequestLogService.record(entity));
        } catch (Exception ex) {
            log.warn("Submit merchant request log failed: {}", ex.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getSignParams(HttpServletRequest request) {
        Object value = request.getAttribute(OpenApiAuthFilter.ATTR_SIGN_PARAMS);
        if (value instanceof Map<?, ?> params) {
            return (Map<String, Object>) params;
        }
        return Map.of();
    }

    private String getSignParam(HttpServletRequest request, String name) {
        return getParam(getSignParams(request), name);
    }

    private String firstNotBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StringUtils.isNotBlank(value)) {
                return value;
            }
        }
        return null;
    }

    private String getParam(Map<String, Object> params, String name) {
        Object value = params.get(name);
        return value == null ? null : StringUtils.trimToNull(String.valueOf(value));
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.isNotBlank(forwarded)) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (StringUtils.isNotBlank(realIp)) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    private Map<String, Object> balanceResponseSummary(BalanceQueryRequest body, List<BalanceResponse> data) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("currency", body == null ? null : body.getCurrency());
        summary.put("count", data == null ? 0 : data.size());
        return summary;
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
}
