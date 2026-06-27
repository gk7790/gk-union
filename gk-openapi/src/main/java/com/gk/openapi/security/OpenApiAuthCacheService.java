package com.gk.openapi.security;

import com.gk.common.enums.SignTypeEnum;
import com.gk.common.openapi.OpenApiAuthCacheEvictor;
import com.gk.common.redis.RedisKeys;
import com.gk.common.redis.RedisUtils;
import com.gk.common.utils.IpPatternUtils;
import com.gk.infra.config.service.GkSysParamsConfigService;
import com.gk.infra.enums.StatusEnum;
import com.gk.merchant.entity.MerchantAppEntity;
import com.gk.merchant.entity.MerchantEntity;
import com.gk.openapi.dao.OpenApiAuthDao;
import com.gk.openapi.error.ApiErrorCode;
import com.gk.openapi.error.ApiException;
import com.gk.openapi.util.ApiSignUtils;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenApiAuthCacheService implements OpenApiAuthCacheEvictor {
    private static final long CACHE_SECONDS = 24 * 60 * 60L;
    private static final long DEFAULT_TIMESTAMP_WINDOW_SECONDS = 300L;

    private final OpenApiAuthDao openApiAuthDao;
    private final RedisUtils redisUtils;
    private final GkSysParamsConfigService configService;

    public ApiReqContext authenticate(String appId,
                                      String clientIp,
                                      Map<String, Object> signParams,
                                      String timestamp,
                                      String nonce,
                                      String signType,
                                      String signature,
                                      String traceId) {
        Snapshot snapshot = getByAppId(appId);
        if (snapshot == null) {
            throw new ApiException(ApiErrorCode.INVALID_APP);
        }

        if (!StatusEnum.NORMAL.code().equals(snapshot.getAppStatus())) {
            throw new ApiException(ApiErrorCode.APP_DISABLED);
        }
        String defaultSignType = StringUtils.defaultIfBlank(configService.openApiConfig().getDefaultSignType(), SignTypeEnum.HMAC_SHA256.code());
        String appSignType = StringUtils.defaultIfBlank(snapshot.getSignType(), defaultSignType);
        if (!supportedSignType(signType) || !signType.equalsIgnoreCase(appSignType)) {
            throw new ApiException(ApiErrorCode.UNSUPPORTED_SIGN_TYPE);
        }
        if (StringUtils.isBlank(snapshot.getApiSecret())) {
            throw new ApiException(ApiErrorCode.INVALID_APP, "App secret is empty");
        }
        if (!IpPatternUtils.matchesAny(clientIp, snapshot.getIpWhitelistPatterns())) {
            throw new ApiException(ApiErrorCode.INVALID_IP);
        }
        if (!Integer.valueOf(1).equals(snapshot.getMerchantStatus())) {
            throw new ApiException(ApiErrorCode.MERCHANT_DISABLED);
        }
        if (!"NORMAL".equalsIgnoreCase(snapshot.getRiskStatus())) {
            throw new ApiException(ApiErrorCode.MERCHANT_DISABLED, "Merchant risk status is not normal");
        }

        validateTimestamp(timestamp);
        if (configService.openApiConfig().isRequireNonce() && StringUtils.isBlank(nonce)) {
            throw new ApiException(ApiErrorCode.INVALID_REQUEST, "request parameters 'nonce' is empty");
        }
        validateNonce(appId, nonce, snapshot.getNonceTtlSeconds());
        validateRateLimit(appId, snapshot.getRateLimitQps());
        validateSortedParamSignature(signParams, signature, snapshot.getApiSecret(), signType);

        return snapshot.toContext(traceId, clientIp);
    }

    public Snapshot getByAppId(String appId) {
        String normalizedAppId = StringUtils.trimToNull(appId);
        if (normalizedAppId == null) {
            return null;
        }

        String cacheKey = RedisKeys.getOpenApiAuthKey(normalizedAppId);
        Snapshot cached = getCached(cacheKey);
        if (cached != null) {
            return cached;
        }

        Snapshot snapshot = loadByAppId(normalizedAppId);
        if (snapshot != null) {
            cache(cacheKey, snapshot);
        }
        return snapshot;
    }

    @Override
    public void evictByAppId(String appId) {
        String normalizedAppId = StringUtils.trimToNull(appId);
        if (normalizedAppId == null) {
            return;
        }
        try {
            redisUtils.delete(RedisKeys.getOpenApiAuthKey(normalizedAppId));
            redisUtils.delete(RedisKeys.getOpenApiMerchantAppKey(normalizedAppId));
        } catch (Exception ex) {
            log.warn("Evict OpenAPI auth cache failed, appId={}, err={}", normalizedAppId, ex.getMessage());
        }
    }

    @Override
    public void evictByMerchant(Long tenantId, Long merchantId) {
        if (tenantId == null || merchantId == null) {
            return;
        }
        openApiAuthDao.selectAppIdsByMerchant(tenantId, merchantId)
                .forEach(this::evictByAppId);
    }

    private Snapshot loadByAppId(String appId) {
        OpenApiAuthSnapshotRow row = openApiAuthDao.selectAuthSnapshotByAppId(appId);
        if (row == null) {
            return null;
        }

        Snapshot snapshot = Snapshot.from(row);
        snapshot.setIpWhitelistPatterns(openApiAuthDao.selectIpWhitelistPatterns(
                row.getTenantId(),
                row.getMerchantId()
        ));
        return snapshot;
    }

    private Snapshot getCached(String cacheKey) {
        try {
            return redisUtils.get(cacheKey, Snapshot.class);
        } catch (Exception ex) {
            log.warn("Get OpenAPI auth cache failed, key={}, err={}", cacheKey, ex.getMessage());
            return null;
        }
    }

    private void cache(String cacheKey, Snapshot snapshot) {
        try {
            redisUtils.set(cacheKey, snapshot, CACHE_SECONDS);
        } catch (Exception ex) {
            log.warn("Set OpenAPI auth cache failed, key={}, err={}", cacheKey, ex.getMessage());
        }
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
        if (!redisUtils.tryLockStrict(nonceKey, ttl)) {
            throw new ApiException(ApiErrorCode.REPLAY_REQUEST);
        }
    }

    private void validateRateLimit(String appId, Integer rateLimitQps) {
        int qps = rateLimitQps == null || rateLimitQps <= 0 ? 50 : rateLimitQps;
        String limitQpsKey = RedisKeys.getApiLimitQpsKey(appId, Instant.now().getEpochSecond());
        if (redisUtils.getIncrement(limitQpsKey, 2) > qps) {
            throw new ApiException(ApiErrorCode.INVALID_REQUEST, "Rate limit exceeded");
        }
    }

    private boolean supportedSignType(String signType) {
        return SignTypeEnum.HMAC_SHA256.matches(signType) || SignTypeEnum.MD5.matches(signType);
    }

    private void validateSortedParamSignature(Map<String, Object> params,
                                              String signature,
                                              String apiSecret,
                                              String signType) {
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

    @Data
    public static class Snapshot {
        private Long tenantId;
        private Long merchantId;
        private Long merchantAppId;
        private String appId;
        private String apiSecret;
        private String signType;
        private Integer appStatus;
        private Integer nonceTtlSeconds;
        private Integer rateLimitQps;
        private String appEnv;
        private String merchantNo;
        private Integer merchantStatus;
        private String riskStatus;
        private String defaultCurrency;
        private String countryCode;
        private List<String> ipWhitelistPatterns;

        static Snapshot from(OpenApiAuthSnapshotRow row) {
            Snapshot snapshot = new Snapshot();
            snapshot.setTenantId(row.getTenantId());
            snapshot.setMerchantId(row.getMerchantId());
            snapshot.setMerchantAppId(row.getMerchantAppId());
            snapshot.setAppId(row.getAppId());
            snapshot.setApiSecret(row.getApiSecret());
            snapshot.setSignType(row.getSignType());
            snapshot.setAppStatus(row.getAppStatus());
            snapshot.setNonceTtlSeconds(row.getNonceTtlSeconds());
            snapshot.setRateLimitQps(row.getRateLimitQps());
            snapshot.setAppEnv(row.getAppEnv());
            snapshot.setMerchantNo(row.getMerchantNo());
            snapshot.setMerchantStatus(row.getMerchantStatus());
            snapshot.setRiskStatus(row.getRiskStatus());
            snapshot.setDefaultCurrency(row.getDefaultCurrency());
            snapshot.setCountryCode(row.getCountryCode());
            return snapshot;
        }

        ApiReqContext toContext(String traceId, String clientIp) {
            MerchantAppEntity app = new MerchantAppEntity();
            app.setId(merchantAppId);
            app.setTenantId(tenantId);
            app.setMerchantId(merchantId);
            app.setAppId(appId);
            app.setApiSecret(apiSecret);
            app.setSignType(signType);
            app.setStatus(appStatus);
            app.setNonceTtlSeconds(nonceTtlSeconds);
            app.setRateLimitQps(rateLimitQps);
            app.setAppEnv(appEnv);

            MerchantEntity merchant = new MerchantEntity();
            merchant.setId(merchantId);
            merchant.setTenantId(tenantId);
            merchant.setMerchantNo(merchantNo);
            merchant.setStatus(merchantStatus);
            merchant.setRiskStatus(riskStatus);
            merchant.setDefaultCurrency(defaultCurrency);
            merchant.setCountryCode(countryCode);

            return ApiReqContext.builder()
                    .tenantId(tenantId)
                    .merchantId(merchantId)
                    .merchantNo(merchantNo)
                    .merchantAppId(merchantAppId)
                    .appId(appId)
                    .traceId(traceId)
                    .clientIp(clientIp)
                    .merchant(merchant)
                    .merchantApp(app)
                    .build();
        }
    }
}
