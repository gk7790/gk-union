package com.gk.config;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gk.merchant.cache.OpenApiAuthCacheEvictor;
import com.gk.merchant.cache.OpenApiCacheKeys;
import com.gk.common.redis.RedisKeys;
import com.gk.common.redis.RedisUtils;
import com.gk.merchant.dao.MerchantAppDao;
import com.gk.merchant.entity.MerchantAppEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminOpenApiAuthCacheEvictor implements OpenApiAuthCacheEvictor {
    private final MerchantAppDao merchantAppDao;
    private final RedisUtils redisUtils;

    @Override
    public void evictByAppId(String appId) {
        String normalizedAppId = StringUtils.trimToNull(appId);
        if (normalizedAppId == null) {
            return;
        }
        try {
            redisUtils.delete(OpenApiCacheKeys.auth(normalizedAppId));
            redisUtils.delete(OpenApiCacheKeys.merchantApp(normalizedAppId));
        } catch (Exception ex) {
            log.warn("Evict OpenAPI auth cache failed, appId={}, err={}", normalizedAppId, ex.getMessage());
        }
    }

    @Override
    public void evictByMerchant(Long tenantId, Long merchantId) {
        if (tenantId == null || merchantId == null) {
            return;
        }
        List<MerchantAppEntity> apps = merchantAppDao.selectList(new QueryWrapper<MerchantAppEntity>()
                .select("app_id")
                .eq("tenant_id", tenantId)
                .eq("merchant_id", merchantId));
        apps.stream()
                .map(MerchantAppEntity::getAppId)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .forEach(this::evictByAppId);
    }
}
