package com.gk.openapi.security;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gk.common.context.ReqContext;
import com.gk.common.context.ReqContextHolder;
import com.gk.common.redis.RedisUtils;
import com.gk.merchant.dao.MerchantAppDao;
import com.gk.merchant.dao.MerchantDao;
import com.gk.merchant.entity.MerchantAppEntity;
import com.gk.merchant.entity.MerchantEntity;
import com.gk.openapi.dto.ApiR;
import com.gk.openapi.error.ApiErrorCode;
import com.gk.openapi.error.OpenApiException;
import com.gk.openapi.util.IpWhitelistUtils;
import com.gk.openapi.util.OpenApiSignatureUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Order(20)
@Component
@RequiredArgsConstructor
public class OpenApiAuthFilter extends OncePerRequestFilter {
    public static final String HEADER_APP_ID = "X-App-Id";
    public static final String HEADER_TIMESTAMP = "X-Timestamp";
    public static final String HEADER_NONCE = "X-Nonce";
    public static final String HEADER_SIGN_TYPE = "X-Sign-Type";
    public static final String HEADER_SIGNATURE = "X-Signature";
    public static final String HEADER_REQUEST_ID = "X-Request-Id";

    private static final long DEFAULT_TIMESTAMP_WINDOW_SECONDS = 300L;
    private static final String SIGN_TYPE_HMAC_SHA256 = "HMAC_SHA256";

    private final MerchantAppDao merchantAppDao;
    private final MerchantDao merchantDao;
    private final RedisUtils redisUtils;
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/v1/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(request);
        String requestId = header(cachedRequest, HEADER_REQUEST_ID);
        if (StringUtils.isBlank(requestId)) {
            requestId = UUID.randomUUID().toString();
        }

        try {
            OpenApiRequestContext context = authenticate(cachedRequest, requestId);
            OpenApiRequestContextHolder.set(context);
            ReqContextHolder.set(ReqContext.builder()
                    .model("openapi")
                    .tenantId(context.getTenantId())
                    .traceId(context.getTraceId())
                    .requestId(requestId)
                    .ip(context.getClientIp())
                    .uri(cachedRequest.getRequestURI())
                    .method(cachedRequest.getMethod())
                    .lang(context.getMerchant().getLang())
                    .timezone(context.getMerchant().getTimezone())
                    .sAdmin(false)
                    .build());
            filterChain.doFilter(cachedRequest, response);
        } catch (OpenApiException ex) {
            writeError(response, ex.getErrorCode().name(), ex.getMessage(), requestId);
        } catch (Exception ex) {
            writeError(response, ApiErrorCode.SYSTEM_ERROR.name(), ApiErrorCode.SYSTEM_ERROR.getMessage(), requestId);
        } finally {
            OpenApiRequestContextHolder.clear();
            ReqContextHolder.clear();
        }
    }

    private OpenApiRequestContext authenticate(CachedBodyHttpServletRequest request, String requestId) {
        String appId = requireHeader(request, HEADER_APP_ID);
        String timestamp = requireHeader(request, HEADER_TIMESTAMP);
        String nonce = requireHeader(request, HEADER_NONCE);
        String signType = StringUtils.defaultIfBlank(header(request, HEADER_SIGN_TYPE), SIGN_TYPE_HMAC_SHA256);
        String signature = requireHeader(request, HEADER_SIGNATURE);

        MerchantAppEntity app = merchantAppDao.selectOne(
                new QueryWrapper<MerchantAppEntity>().eq("app_id", appId).last("limit 1")
        );
        if (app == null) {
            throw new OpenApiException(ApiErrorCode.INVALID_APP);
        }
        if (!Integer.valueOf(1).equals(app.getStatus())) {
            throw new OpenApiException(ApiErrorCode.APP_DISABLED);
        }
        if (!SIGN_TYPE_HMAC_SHA256.equalsIgnoreCase(signType) || !SIGN_TYPE_HMAC_SHA256.equalsIgnoreCase(app.getSignType())) {
            throw new OpenApiException(ApiErrorCode.UNSUPPORTED_SIGN_TYPE);
        }
        if (StringUtils.isBlank(app.getApiSecret())) {
            throw new OpenApiException(ApiErrorCode.INVALID_APP, "App secret is empty");
        }

        MerchantEntity merchant = merchantDao.selectById(app.getMerchantId());
        if (merchant == null || !Integer.valueOf(1).equals(merchant.getStatus())) {
            throw new OpenApiException(ApiErrorCode.MERCHANT_DISABLED);
        }
        if (!"NORMAL".equalsIgnoreCase(merchant.getRiskStatus())) {
            throw new OpenApiException(ApiErrorCode.MERCHANT_DISABLED, "Merchant risk status is not normal");
        }

        String clientIp = getClientIp(request);
        if (!IpWhitelistUtils.allowed(clientIp, app.getIpWhitelistJson())) {
            throw new OpenApiException(ApiErrorCode.INVALID_IP);
        }

        validateTimestamp(timestamp);
        validateNonce(appId, nonce, app.getNonceTtlSeconds());
        validateRateLimit(appId, app.getRateLimitQps());
        validateSignature(request, timestamp, nonce, signature, app.getApiSecret());

        return OpenApiRequestContext.builder()
                .tenantId(app.getTenantId())
                .merchantId(app.getMerchantId())
                .merchantNo(merchant.getMerchantNo())
                .merchantAppId(app.getId())
                .appId(app.getAppId())
                .requestId(requestId)
                .traceId(requestId)
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
                throw new OpenApiException(ApiErrorCode.INVALID_TIMESTAMP);
            }
        } catch (NumberFormatException e) {
            throw new OpenApiException(ApiErrorCode.INVALID_TIMESTAMP);
        }
    }

    private void validateNonce(String appId, String nonce, Integer nonceTtlSeconds) {
        int ttl = nonceTtlSeconds == null || nonceTtlSeconds <= 0 ? 300 : nonceTtlSeconds;
        boolean locked = redisUtils.tryLock("openapi:nonce:" + appId + ":" + nonce, ttl);
        if (!locked) {
            throw new OpenApiException(ApiErrorCode.REPLAY_REQUEST);
        }
    }

    private void validateRateLimit(String appId, Integer rateLimitQps) {
        int qps = rateLimitQps == null || rateLimitQps <= 0 ? 50 : rateLimitQps;
        long count = redisUtils.getIncrement("openapi:rate:" + appId + ":" + Instant.now().getEpochSecond(), 2);
        if (count > qps) {
            throw new OpenApiException(ApiErrorCode.INVALID_REQUEST, "Rate limit exceeded");
        }
    }

    private void validateSignature(CachedBodyHttpServletRequest request, String timestamp, String nonce, String signature, String apiSecret) {
        String signText = OpenApiSignatureUtils.buildSignText(request, request.getBody(), timestamp, nonce);
        String expected = OpenApiSignatureUtils.hmacSha256Hex(signText, apiSecret).toLowerCase(Locale.ROOT);
        if (!OpenApiSignatureUtils.secureEquals(expected, signature.toLowerCase(Locale.ROOT))) {
            throw new OpenApiException(ApiErrorCode.INVALID_SIGNATURE);
        }
    }

    private String requireHeader(HttpServletRequest request, String name) {
        String value = header(request, name);
        if (StringUtils.isBlank(value)) {
            throw new OpenApiException(ApiErrorCode.INVALID_REQUEST, "Missing header: " + name);
        }
        return value;
    }

    private String header(HttpServletRequest request, String name) {
        return StringUtils.trimToNull(request.getHeader(name));
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

    private void writeError(HttpServletResponse response, String code, String message, String requestId) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json;charset=UTF-8");
        String body = objectMapper.writeValueAsString(ApiR.error(code, message, requestId));
        response.getOutputStream().write(body.getBytes(StandardCharsets.UTF_8));
    }
}
