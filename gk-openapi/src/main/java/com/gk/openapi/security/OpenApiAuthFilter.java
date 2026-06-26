package com.gk.openapi.security;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.gk.common.context.ReqContext;
import com.gk.common.context.ReqContextHolder;
import com.gk.common.enums.SignTypeEnum;
import com.gk.common.enums.SubjectTypeEnum;
import com.gk.openapi.error.ApiErrorCode;
import com.gk.openapi.error.ApiException;
import com.gk.openapi.log.MerchantRequestLogger;
import com.gk.openapi.tools.ApiR;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Order(20)
@Component
@RequiredArgsConstructor
@Slf4j
public class OpenApiAuthFilter extends OncePerRequestFilter {
    public static final String ATTR_SIGN_PARAMS = OpenApiAuthFilter.class.getName() + ".SIGN_PARAMS";

    public static final String PARAM_APP_ID = "app_id";
    public static final String PARAM_TIMESTAMP = "timestamp";
    public static final String PARAM_NONCE = "nonce";
    public static final String PARAM_SIGN_TYPE = "sign_type";
    public static final String PARAM_SIGN = "sign";

    private final OpenApiAuthCacheService openApiAuthCacheService;
    private final MerchantRequestLogger merchantRequestLogger;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri == null || (!uri.startsWith("/api/v1/") && !uri.startsWith("/open-api/"));
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws IOException {
        CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(request);
        String traceId = UUID.randomUUID().toString();

        try {
            ApiReqContext context = authenticate(cachedRequest, traceId);
            ApiReqContextHolder.set(context);
            ReqContextHolder.set(toReqContext(context));
            filterChain.doFilter(cachedRequest, response);
        } catch (ApiException ex) {
            recordRejectedRequest(cachedRequest, traceId, ex.getErrorCode().name(), ex.getMessage());
            writeError(response, ex.getErrorCode().name(), ex.getMessage());
        } catch (RedisConnectionFailureException ex) {
            log.error("OpenAPI Redis unavailable, traceId={}, uri={}", traceId, cachedRequest.getRequestURI(), ex);
            recordRejectedRequest(cachedRequest, traceId, ApiErrorCode.SERVICE_NOT_READY.name(), ApiErrorCode.SERVICE_NOT_READY.getMessage());
            writeError(response, ApiErrorCode.SERVICE_NOT_READY.name(), ApiErrorCode.SERVICE_NOT_READY.getMessage());
        } catch (Exception ex) {
            log.error("OpenAPI auth filter failed, traceId={}, uri={}", traceId, cachedRequest.getRequestURI(), ex);
            recordRejectedRequest(cachedRequest, traceId, ApiErrorCode.SYSTEM_ERROR.name(), ApiErrorCode.SYSTEM_ERROR.getMessage());
            writeError(response, ApiErrorCode.SYSTEM_ERROR.name(), ApiErrorCode.SYSTEM_ERROR.getMessage());
        } finally {
            ApiReqContextHolder.clear();
            ReqContextHolder.clear();
        }
    }

    private ReqContext toReqContext(ApiReqContext context) {
        return ReqContext.builder()
                .tenantId(context.getTenantId())
                .merchantId(context.getMerchantId())
                .subjectId(context.getMerchantId())
                .subjectType(SubjectTypeEnum.MERCHANT.code())
                .traceId(context.getTraceId())
                .ip(context.getClientIp())
                .build();
    }

    private ApiReqContext authenticate(CachedBodyHttpServletRequest request, String traceId) {
        Map<String, Object> signParams = extractSignParams(request);
        request.setAttribute(ATTR_SIGN_PARAMS, signParams);
        String appId = requireParam(signParams, PARAM_APP_ID);
        String timestamp = requireParam(signParams, PARAM_TIMESTAMP);
        String nonce = getParam(signParams, PARAM_NONCE);
        String signType = StringUtils.defaultIfBlank(getParam(signParams, PARAM_SIGN_TYPE), SignTypeEnum.MD5.code());
        String signature = requireParam(signParams, PARAM_SIGN);

        return openApiAuthCacheService.authenticate(
                appId,
                getClientIp(request),
                signParams,
                timestamp,
                nonce,
                signType,
                signature,
                traceId
        );
    }

    private Map<String, Object> extractSignParams(CachedBodyHttpServletRequest request) {
        String contentType = StringUtils.defaultString(request.getContentType()).toLowerCase(Locale.ROOT);
        String body = new String(request.getBody(), StandardCharsets.UTF_8);
        if (StringUtils.isBlank(body)) {
            return Map.of();
        }
        if (contentType.contains("application/x-www-form-urlencoded")) {
            return parseFormBody(body);
        }
        if (contentType.contains("application/json")) {
            return parseJsonBody(body);
        }
        return Map.of();
    }

    private Map<String, Object> parseJsonBody(String body) {
        try {
            Map<String, Object> params = JSON.parseObject(body, new TypeReference<LinkedHashMap<String, Object>>() {
            });
            return params == null ? Map.of() : params;
        } catch (Exception e) {
            throw new ApiException(ApiErrorCode.INVALID_REQUEST, "Invalid JSON body");
        }
    }

    private Map<String, Object> parseFormBody(String body) {
        Map<String, Object> params = new LinkedHashMap<>();
        for (String pair : body.split("&")) {
            if (StringUtils.isBlank(pair)) {
                continue;
            }
            int idx = pair.indexOf('=');
            String key = decodeFormValue(idx >= 0 ? pair.substring(0, idx) : pair);
            String value = decodeFormValue(idx >= 0 ? pair.substring(idx + 1) : "");
            params.put(key, value);
        }
        return params;
    }

    private String decodeFormValue(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private String requireParam(Map<String, Object> params, String name) {
        String value = getParam(params, name);
        if (StringUtils.isBlank(value)) {
            throw new ApiException(ApiErrorCode.INVALID_REQUEST, "request parameters '" + name + "' is empty");
        }
        return value;
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

    private void writeError(HttpServletResponse response, String code, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json;charset=UTF-8");
        String body = JSON.toJSONString(ApiR.error(code, message));
        response.getOutputStream().write(body.getBytes(StandardCharsets.UTF_8));
    }

    private void recordRejectedRequest(CachedBodyHttpServletRequest request, String traceId, String code, String message) {
        try {
            String rawBody = new String(request.getBody(), StandardCharsets.UTF_8);
            merchantRequestLogger.authRejected(request, traceId, rawBody, code, message);
        } catch (Exception ignored) {
            // Do not block OpenAPI authentication response because of audit log failure.
        }
    }
}
