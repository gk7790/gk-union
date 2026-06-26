package com.gk.payment.plan;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class PayinPlanCache {
    /**
     * PayinPlan 是配置解析结果缓存，不是订单结果缓存     * <p>
     * TTL 保持较短，并在相关配置保修改/删除时主动清空，避免商户费率PSP 配置变更后继续使用旧方案     */
    private static final long PLAN_CACHE_TTL_MILLIS = Duration.ofSeconds(30).toMillis();

    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();

    public PayinPlan get(String key) {
        CacheEntry entry = cache.get(key);
        if (entry == null) {
            return null;
        }
        if (entry.expiresAtMillis() <= System.currentTimeMillis()) {
            // 惰性清理过期缓存，避免后台定时任务带来额外复杂度
                        cache.remove(key);
            return null;
        }
        return entry.plan();
    }

    public void put(String key, PayinPlan plan) {
        cache.put(key, new CacheEntry(plan, System.currentTimeMillis() + PLAN_CACHE_TTL_MILLIS));
    }

    public void evictAll() {
        cache.clear();
    }

    private record CacheEntry(PayinPlan plan, long expiresAtMillis) {
    }
}
