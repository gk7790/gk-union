package com.gk.payment.plan;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gk.common.redis.RedisKeys;
import com.gk.common.redis.RedisUtils;
import com.gk.payment.dao.PaymentPlanBucketDao;
import com.gk.payment.dao.PaymentPlanCatalogDao;
import com.gk.payment.dao.PaymentPlanRouteOptionDao;
import com.gk.payment.entity.PaymentPlanBucketEntity;
import com.gk.payment.entity.PaymentPlanCatalogEntity;
import com.gk.payment.entity.PaymentPlanRouteOptionEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 支付决策表缓存服务�? * <p>
 * 商户 API 运行时只依赖这个服务读取 ACTIVE 决策表；后台发布服务后续只需要清理对�?key 即可�? */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentPlanCacheService {
    private static final long LOCAL_CACHE_TTL_MILLIS = Duration.ofMinutes(5).toMillis();
    private static final long REDIS_CACHE_TTL_SECONDS = Duration.ofMinutes(30).toSeconds();

    private final RedisUtils redisUtils;
    private final PaymentPlanCatalogDao paymentPlanCatalogDao;
    private final PaymentPlanBucketDao paymentPlanBucketDao;
    private final PaymentPlanRouteOptionDao paymentPlanRouteOptionDao;
    private final ConcurrentHashMap<String, CacheEntry> localCache = new ConcurrentHashMap<>();

    public Optional<PaymentPlanCatalog> findActive(PaymentPlanKey key) {
        String localKey = key.localKey();
        PaymentPlanCatalog local = getLocal(localKey);
        if (local != null) {
            return Optional.of(local);
        }

        PaymentPlanCatalog cached = getRedis(key.redisKey());
        if (cached != null) {
            putLocal(localKey, cached);
            return Optional.of(cached);
        }

        PaymentPlanCatalog loaded = loadActiveFromDb(key);
        if (loaded == null) {
            return Optional.empty();
        }
        // DB 回源成功后同时回�?Redis 和本地缓存，后续商户 API 请求不再打到数据库�?        putRedis(key.redisKey(), loaded);
        putLocal(localKey, loaded);
        return Optional.of(loaded);
    }

    public void evict(PaymentPlanKey key) {
        localCache.remove(key.localKey());
        try {
            redisUtils.delete(key.redisKey());
        } catch (Exception ex) {
            log.warn("Evict payment plan redis key failed: {}", ex.getMessage());
        }
    }

    public void evictAll() {
        localCache.clear();
        try {
            Set<String> keys = redisUtils.keys(RedisKeys.getPaymentPlanActivePattern());
            if (keys != null && !keys.isEmpty()) {
                redisUtils.delete(keys);
            }
        } catch (Exception ex) {
            log.warn("Evict payment plan redis cache failed: {}", ex.getMessage());
        }
    }

    private PaymentPlanCatalog getLocal(String localKey) {
        CacheEntry entry = localCache.get(localKey);
        if (entry == null) {
            return null;
        }
        if (entry.expiresAtMillis() <= System.currentTimeMillis()) {
            localCache.remove(localKey);
            return null;
        }
        return entry.catalog();
    }

    private PaymentPlanCatalog getRedis(String redisKey) {
        try {
            return redisUtils.get(redisKey, PaymentPlanCatalog.class);
        } catch (Exception ex) {
            log.warn("Read payment plan redis cache failed: {}", ex.getMessage());
            return null;
        }
    }

    private void putLocal(String localKey, PaymentPlanCatalog catalog) {
        localCache.put(localKey, new CacheEntry(catalog, System.currentTimeMillis() + LOCAL_CACHE_TTL_MILLIS));
    }

    private void putRedis(String redisKey, PaymentPlanCatalog catalog) {
        try {
            redisUtils.set(redisKey, catalog, REDIS_CACHE_TTL_SECONDS);
        } catch (Exception ex) {
            log.warn("Write payment plan redis cache failed: {}", ex.getMessage());
        }
    }

    private PaymentPlanCatalog loadActiveFromDb(PaymentPlanKey key) {
        PaymentPlanCatalogEntity catalog = selectActiveCatalog(key);
        if (catalog == null) {
            return null;
        }

        List<PaymentPlanBucketEntity> buckets = paymentPlanBucketDao.selectList(new QueryWrapper<PaymentPlanBucketEntity>()
                .eq("tenant_id", catalog.getTenantId())
                .eq("catalog_id", catalog.getId())
                .orderByAsc("sort")
                .orderByAsc("bucket_start_amount"));
        if (buckets.isEmpty()) {
            return null;
        }

        List<PaymentPlanRouteOptionEntity> routeOptions = paymentPlanRouteOptionDao.selectList(new QueryWrapper<PaymentPlanRouteOptionEntity>()
                .eq("tenant_id", catalog.getTenantId())
                .eq("catalog_id", catalog.getId())
                .orderByAsc("bucket_id")
                .orderByAsc("priority")
                .orderByAsc("fallback_order")
                .orderByAsc("sort"));
        Map<Long, List<PaymentPlanRouteOptionEntity>> routeOptionMap = routeOptions.stream()
                .collect(Collectors.groupingBy(
                        PaymentPlanRouteOptionEntity::getBucketId,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        PaymentPlanCatalog result = new PaymentPlanCatalog();
        result.setCatalog(catalog);
        result.setBuckets(buckets.stream()
                .map(bucket -> planBucket(bucket, routeOptionMap.get(bucket.getId())))
                .toList());
        return result;
    }

    private PaymentPlanCatalogEntity selectActiveCatalog(PaymentPlanKey key) {
        boolean hasApp = key.merchantAppId() != null && key.merchantAppId() > 0;

        List<Long> merchantAppIds = hasApp
                ? List.of(key.merchantAppId(), 0L)
                : List.of(0L);

        QueryWrapper<PaymentPlanCatalogEntity> wrapper = new QueryWrapper<PaymentPlanCatalogEntity>()
                .eq("tenant_id", key.tenantId())
                .eq("merchant_id", key.merchantId())
                .eq("direction", key.direction())
                .eq("currency", key.currency())
                .eq("method_code", key.methodCode())
                .eq("status", PaymentPlanStatus.ACTIVE)
                .in("merchant_app_id", merchantAppIds);
        if (StringUtils.isNotBlank(key.countryCode())) {
            wrapper.and(item -> item.eq("country_code", key.countryCode()).or().isNull("country_code").or().eq("country_code", ""));
            wrapper.orderByDesc("country_code");
        }
        wrapper
                .orderByDesc("version")
                .orderByDesc("id");
        wrapper.last("limit 1");
        return paymentPlanCatalogDao.selectOne(wrapper);
    }

    private PaymentPlanBucket planBucket(PaymentPlanBucketEntity bucket, List<PaymentPlanRouteOptionEntity> routeOptions) {
        PaymentPlanBucket planBucket = new PaymentPlanBucket();
        planBucket.setBucket(bucket);
        planBucket.setRouteOptions(routeOptions == null ? Collections.emptyList() : routeOptions);
        return planBucket;
    }

    private record CacheEntry(PaymentPlanCatalog catalog, long expiresAtMillis) {
    }
}
