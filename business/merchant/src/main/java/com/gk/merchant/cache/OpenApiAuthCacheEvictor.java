package com.gk.merchant.cache;

/**
 * OpenAPI 鉴权快照缓存失效接口。
 * <p>
 * 由 gk-ledger 实现；其他模块通过 {@link org.springframework.beans.factory.ObjectProvider} 按需调用。
 */
public interface OpenApiAuthCacheEvictor {

    void evictByAppId(String appId);

    void evictByMerchant(Long tenantId, Long merchantId);
}
