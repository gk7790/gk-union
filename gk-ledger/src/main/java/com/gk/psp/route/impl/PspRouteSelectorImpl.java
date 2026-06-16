package com.gk.psp.route.impl;

import com.gk.common.enums.PayDirectionEnum;
import com.gk.infra.enums.StatusEnum;
import com.gk.openapi.error.ApiErrorCode;
import com.gk.openapi.error.ApiException;
import com.gk.payment.entity.PayOrderEntity;
import com.gk.payment.entity.PayoutOrderEntity;
import com.gk.psp.dao.PspAccountDao;
import com.gk.psp.dao.PspMethodDao;
import com.gk.psp.dao.PspProviderDao;
import com.gk.psp.dao.PspRouteRuleDao;
import com.gk.psp.entity.PspAccountEntity;
import com.gk.psp.entity.PspMethodEntity;
import com.gk.psp.entity.PspProviderEntity;
import com.gk.psp.entity.PspRouteRuleEntity;
import com.gk.psp.route.PspRouteResult;
import com.gk.psp.route.PspRouteSelector;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class PspRouteSelectorImpl implements PspRouteSelector {
    private static final long RESOURCE_CACHE_TTL_MILLIS = Duration.ofSeconds(30).toMillis();

    private final PspRouteRuleDao pspRouteRuleDao;
    private final PspProviderDao pspProviderDao;
    private final PspMethodDao pspMethodDao;
    private final PspAccountDao pspAccountDao;
    private final ConcurrentHashMap<Long, CacheEntry<PspProviderEntity>> providerCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, CacheEntry<PspMethodEntity>> methodCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, CacheEntry<PspAccountEntity>> accountCache = new ConcurrentHashMap<>();

    @Override
    public PspRouteResult selectPayin(PayOrderEntity order) {
        return select(
                order.getTenantId(),
                order.getMerchantId(),
                order.getMerchantAppId(),
                order.getCountryCode(),
                order.getCurrency(),
                order.getMethodCode(),
                order.getAmount(),
                PayDirectionEnum.PAYIN.code()
        );
    }

    @Override
    public PspRouteResult selectPayout(PayoutOrderEntity order) {
        return select(
                order.getTenantId(),
                order.getMerchantId(),
                order.getMerchantAppId(),
                order.getCountryCode(),
                order.getCurrency(),
                order.getMethodCode(),
                order.getAmount(),
                PayDirectionEnum.PAYOUT.code()
        );
    }

    private PspRouteResult select(
            Long tenantId,
            Long merchantId,
            Long merchantAppId,
            String countryCode,
            String currency,
            String methodCode,
            java.math.BigDecimal amount,
            String direction
    ) {
        PspRouteRuleEntity rule = pspRouteRuleDao.selectBestRouteRuleForOrder(
                tenantId,
                merchantId,
                merchantAppId,
                countryCode,
                currency,
                methodCode,
                amount,
                direction,
                LocalTime.now(),
                StatusEnum.NORMAL.code()
        );
        if (rule == null) {
            throw new ApiException(ApiErrorCode.UNSUPPORTED_METHOD, "No available PSP route");
        }
        PspProviderEntity provider = requireProvider(rule.getPspId(), direction);
        PspMethodEntity method = requireMethod(rule.getPspMethodId());
        PspAccountEntity account = requirePspAccount(rule.getPspAccountId());

        PspRouteResult result = new PspRouteResult();
        result.setRouteRuleId(rule.getId());
        result.setPspId(provider.getId());
        result.setPspCode(provider.getPspCode());
        result.setPspBaseUrl(provider.getBaseUrl());
        result.setProviderConfigJson(provider.getConfigJson());
        result.setPspMethodId(method.getId());
        result.setPspMethodCode(method.getPspMethodCode());
        result.setMethodConfigJson(method.getConfigJson());
        result.setPspAccountId(account.getId());
        result.setPspAccountNo(account.getPspAccountNo());
        result.setPspAccountApiKey(account.getApiKey());
        result.setPspAccountApiSecret(account.getApiSecret());
        result.setAccountConfigJson(account.getConfigJson());
        return result;
    }

    private PspProviderEntity requireProvider(Long pspId, String direction) {
        PspProviderEntity provider = cached(providerCache, pspId, pspProviderDao::selectById);
        if (provider == null || !StatusEnum.NORMAL.code().equals(provider.getStatus())) {
            throw new ApiException(ApiErrorCode.UNSUPPORTED_METHOD, "PSP provider is not available");
        }
        boolean supported = PayDirectionEnum.PAYOUT.code().equals(direction)
                ? Integer.valueOf(1).equals(provider.getSupportPayout())
                : Integer.valueOf(1).equals(provider.getSupportPayin());
        if (!supported) {
            throw new ApiException(ApiErrorCode.UNSUPPORTED_METHOD, "PSP provider is not available");
        }
        return provider;
    }

    private PspMethodEntity requireMethod(Long pspMethodId) {
        PspMethodEntity method = cached(methodCache, pspMethodId, pspMethodDao::selectById);
        if (method == null || !StatusEnum.NORMAL.code().equals(method.getStatus())) {
            throw new ApiException(ApiErrorCode.UNSUPPORTED_METHOD, "PSP method is not available");
        }
        return method;
    }

    private PspAccountEntity requirePspAccount(Long pspAccountId) {
        PspAccountEntity account = cached(accountCache, pspAccountId, pspAccountDao::selectById);
        if (account == null || !StatusEnum.NORMAL.code().equals(account.getStatus())) {
            throw new ApiException(ApiErrorCode.UNSUPPORTED_METHOD, "PSP account is not available");
        }
        return account;
    }

    private <T> T cached(ConcurrentHashMap<Long, CacheEntry<T>> cache, Long id, Function<Long, T> loader) {
        if (id == null) {
            return null;
        }
        long now = System.currentTimeMillis();
        CacheEntry<T> cached = cache.get(id);
        if (cached != null && cached.expiresAtMillis() > now) {
            return cached.value();
        }
        T value = loader.apply(id);
        if (value != null) {
            cache.put(id, new CacheEntry<>(value, now + RESOURCE_CACHE_TTL_MILLIS));
        } else {
            cache.remove(id);
        }
        return value;
    }

    private record CacheEntry<T>(T value, long expiresAtMillis) {
    }
}
