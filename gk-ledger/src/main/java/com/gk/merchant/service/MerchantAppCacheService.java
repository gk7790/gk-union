package com.gk.merchant.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gk.common.redis.RedisKeys;
import com.gk.common.redis.RedisUtils;
import com.gk.merchant.dao.MerchantAppDao;
import com.gk.merchant.entity.MerchantAppEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class MerchantAppCacheService {
    private static final long CACHE_SECONDS = 24 * 60 * 60L;

    private final MerchantAppDao merchantAppDao;
    private final RedisUtils redisUtils;

    public MerchantAppEntity getByAppId(String appId) {
        String normalizedAppId = StringUtils.trimToNull(appId);
        if (normalizedAppId == null) {
            return null;
        }
        String cacheKey = RedisKeys.getOpenApiMerchantAppKey(normalizedAppId);
        MerchantAppEntity cached = getCached(cacheKey);
        if (cached != null) {
            return cached;
        }

        MerchantAppEntity entity = merchantAppDao.selectOne(new QueryWrapper<MerchantAppEntity>()
                .eq("app_id", normalizedAppId)
                .last("limit 1"));
        if (entity != null) {
            cache(cacheKey, entity);
        }
        return entity;
    }

    public void evictByAppId(String appId) {
        String normalizedAppId = StringUtils.trimToNull(appId);
        if (normalizedAppId == null) {
            return;
        }
        try {
            redisUtils.delete(RedisKeys.getOpenApiMerchantAppKey(normalizedAppId));
        } catch (Exception ex) {
            log.warn("Evict merchant app cache failed, appId={}, err={}", normalizedAppId, ex.getMessage());
        }
    }

    public void evictByAppIds(Collection<String> appIds) {
        if (appIds == null || appIds.isEmpty()) {
            return;
        }
        appIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .forEach(this::evictByAppId);
    }

    private MerchantAppEntity getCached(String cacheKey) {
        try {
            return redisUtils.get(cacheKey, MerchantAppEntity.class);
        } catch (Exception ex) {
            log.warn("Get merchant app cache failed, key={}, err={}", cacheKey, ex.getMessage());
            return null;
        }
    }

    private void cache(String cacheKey, MerchantAppEntity entity) {
        try {
            redisUtils.set(cacheKey, entity, CACHE_SECONDS);
        } catch (Exception ex) {
            log.warn("Set merchant app cache failed, key={}, err={}", cacheKey, ex.getMessage());
        }
    }
}
