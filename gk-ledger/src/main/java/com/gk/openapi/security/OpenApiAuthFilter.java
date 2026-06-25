package com.gk.openapi.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gk.common.enums.SignTypeEnum;
import com.gk.common.redis.RedisKeys;
import com.gk.common.redis.RedisUtils;
import com.gk.infra.enums.StatusEnum;
import com.gk.merchant.dao.MerchantDao;
import com.gk.merchant.entity.MerchantAppEntity;
import com.gk.merchant.entity.MerchantEntity;
import com.gk.merchant.service.MerchantAppCacheService;
import com.gk.infra.ipwhitelist.service.MerchantApiIpWhitelistService;
import com.gk.openapi.error.ApiErrorCode;
import com.gk.openapi.error.ApiException;
import com.gk.openapi.log.MerchantRequestLogger;
import com.gk.openapi.tools.ApiR;
import com.gk.openapi.util.ApiSignUtils;
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
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.List;
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

    private static final long DEFAULT_TIMESTAMP_WINDOW_SECONDS = 300L;

    private final MerchantDao merchantDao;
    private final RedisUtils redisUtils;
    private final MerchantRequestLogger merchantRequestLogger;
    private final ObjectMapper objectMapper;
    private final MerchantApiIpWhitelistService merchantApiIpWhitelistService;
    private final MerchantAppCacheService merchantAppCacheService;

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
        }
    }

    private ApiReqContext authenticate(CachedBodyHttpServletRequest request, String traceId) {
        Map<String, Object> signParams = extractSignParams(request);
        request.setAttribute(ATTR_SIGN_PARAMS, signParams);
        String appId = requireParam(signParams, PARAM_APP_ID);
        String timestamp = requireParam(signParams, PARAM_TIMESTAMP);
        String nonce = getParam(signParams, PARAM_NONCE);
        String signType = StringUtils.defaultIfBlank(getParam(signParams, PARAM_SIGN_TYPE), SignTypeEnum.MD5.code());
        String signature = requireParam(signParams, PARAM_SIGN);

        MerchantAppEntity app = merchantAppCacheService.getByAppId(appId);
        if (app == null) {
            throw new ApiException(ApiErrorCode.INVALID_APP);
        }
        if (!StatusEnum.NORMAL.code().equals(app.getStatus())) {
            throw new ApiException(ApiErrorCode.APP_DISABLED);
        }
        String appSignType = StringUtils.defaultIfBlank(app.getSignType(), SignTypeEnum.MD5.code());
        if (!supportedSignType(signType) || !signType.equalsIgnoreCase(appSignType)) {
            throw new ApiException(ApiErrorCode.UNSUPPORTED_SIGN_TYPE);
        }
        if (StringUtils.isBlank(app.getApiSecret())) {
            throw new ApiException(ApiErrorCode.INVALID_APP, "App secret is empty");
        }

        String clientIp = getClientIp(request);
        if (!merchantApiIpWhitelistService.isMerchantApiAllowed(app.getTenantId(), app.getMerchantId(), clientIp)) {
            throw new ApiException(ApiErrorCode.INVALID_IP);
        }

        MerchantEntity merchant = merchantDao.selectById(app.getMerchantId());
        if (merchant == null || !Integer.valueOf(1).equals(merchant.getStatus())) {
            throw new ApiException(ApiErrorCode.MERCHANT_DISABLED);
        }
        if (!"NORMAL".equalsIgnoreCase(merchant.getRiskStatus())) {
            throw new ApiException(ApiErrorCode.MERCHANT_DISABLED, "Merchant risk status is not normal");
        }

        validateTimestamp(timestamp);
        validateNonce(appId, nonce, app.getNonceTtlSeconds());
        validateRateLimit(appId, app.getRateLimitQps());
        validateSortedParamSignature(signParams, signature, app.getApiSecret(), signType);

        return ApiReqContext.builder()
                .tenantId(app.getTenantId())
                .merchantId(app.getMerchantId())
                .merchantNo(merchant.getMerchantNo())
                .merchantAppId(app.getId())
                .appId(app.getAppId())
                .traceId(traceId)
                .clientIp(clientIp)
                .merchant(merchant)
                .merchantApp(app)
                .build();
    }

    private void validateTimestamp(String timestamp) {
        try {
            long requestMillis = Long.parseLong(timestamp);
            long diffMillis = Math.abs(Instant.now().toEpochMilli() - requestMillis);
            if (diffMillis > DEFAULT_TIMESTAMP_WINDOW_SECONDS * 1000) {
                throw new ApiException(ApiErrorCode.INVALID_TIMESTAMP);
            }
        } catch (NumberFormatException e) {
            throw new ApiException(ApiErrorCode.INVALID_TIMESTAMP);
        }
    }

    private void validateNonce(String appId, String nonce, Integer nonceTtlSeconds) {
        if (StringUtils.isBlank(nonce)) {
            return;
        }
        int ttl = nonceTtlSeconds == null || nonceTtlSeconds <= 0 ? 300 : nonceTtlSeconds;
        String nonceKey = RedisKeys.getApiNonceKey(appId, nonce);
        boolean locked = redisUtils.tryLockStrict(nonceKey, ttl);
        if (!locked) {
            throw new ApiException(ApiErrorCode.REPLAY_REQUEST);
        }
    }

    private void validateRateLimit(String appId, Integer rateLimitQps) {
        int qps = rateLimitQps == null || rateLimitQps <= 0 ? 50 : rateLimitQps;
        String limitQpsKey = RedisKeys.getApiLimitQpsKey(appId, Instant.now().getEpochSecond());
        long count = redisUtils.getIncrement(limitQpsKey, 2);
        if (count > qps) {
            throw new ApiException(ApiErrorCode.INVALID_REQUEST, "Rate limit exceeded");
        }
    }

    private boolean supportedSignType(String signType) {
        return SignTypeEnum.HMAC_SHA256.matches(signType) || SignTypeEnum.MD5.matches(signType);
    }

    private void validateSortedParamSignature(Map<String, Object> params, String signature, String apiSecret, String signType) {
        try {
            boolean valid = SignTypeEnum.MD5.matches(signType)
                    ? ApiSignUtils.verifyMd5Sign(params, apiSecret, signature)
                    : ApiSignUtils.verifyHmacSha256Sign(params, apiSecret, signature);
            if (!valid) {
                throw new ApiException(ApiErrorCode.INVALID_SIGNATURE);
            }
        } catch (IllegalArgumentException e) {
            throw new ApiException(ApiErrorCode.INVALID_REQUEST, e.getMessage());
        }
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
            JsonNode root = objectMapper.readTree(body);
            if (root == null || !root.isObject()) {
                return Map.of();
            }
            Map<String, Object> params = new LinkedHashMap<>();
            root.properties().forEach(entry -> params.put(entry.getKey(), jsonNodeToSignValue(entry.getValue())));
            return params;
        } catch (Exception e) {
            throw new ApiException(ApiErrorCode.INVALID_REQUEST, "Invalid JSON body");
        }
    }

    private Object jsonNodeToSignValue(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isObject()) {
            Map<String, Object> map = new LinkedHashMap<>();
            node.properties().forEach(entry -> map.put(entry.getKey(), jsonNodeToSignValue(entry.getValue())));
            return map;
        }
        if (node.isArray()) {
            List<Object> values = new ArrayList<>(node.size());
            node.forEach(item -> values.add(jsonNodeToSignValue(item)));
            return values;
        }
        if (node.isTextual()) {
            return node.asText();
        }
        if (node.isNumber()) {
            return node.decimalValue();
        }
        if (node.isBoolean()) {
            return node.asBoolean();
        }
        return node.asText();
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
        String body = objectMapper.writeValueAsString(ApiR.error(code, message));
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
