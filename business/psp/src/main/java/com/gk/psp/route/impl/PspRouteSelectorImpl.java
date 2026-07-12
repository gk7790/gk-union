package com.gk.psp.route.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gk.payment.domain.enums.PayDirectionEnum;
import com.gk.infra.enums.StatusEnum;
import com.gk.common.exception.ErrorCode;
import com.gk.common.exception.GkException;
import com.gk.psp.dao.PspAccountDao;
import com.gk.psp.dao.PspBankMappingDao;
import com.gk.psp.dao.PspMethodDao;
import com.gk.psp.dao.PspProviderDao;
import com.gk.psp.dao.PspRouteRuleDao;
import com.gk.psp.callback.support.PspCallbackUrlBuilder;
import com.gk.psp.entity.PspAccountEntity;
import com.gk.psp.entity.PspBankMappingEntity;
import com.gk.psp.entity.PspMethodEntity;
import com.gk.psp.entity.PspProviderEntity;
import com.gk.psp.entity.PspRouteRuleEntity;
import com.gk.psp.request.PspOrderRequest;
import com.gk.psp.route.PspRouteResult;
import com.gk.psp.route.PspRouteSelector;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalTime;
import java.util.Locale;
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
    private final PspBankMappingDao pspBankMappingDao;
    private final PspCallbackUrlBuilder callbackUrlBuilder;
    private final ConcurrentHashMap<Long, CacheEntry<PspProviderEntity>> providerCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, CacheEntry<PspMethodEntity>> methodCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, CacheEntry<PspAccountEntity>> accountCache = new ConcurrentHashMap<>();

    @Override
    public PspRouteResult selectPayin(PspOrderRequest order) {
        return select(
                order.getTenantId(),
                order.getMerchantId(),
                order.getMerchantAppId(),
                order.getCountryCode(),
                order.getCurrency(),
                order.getMethodCode(),
                null,
                order.getAmount(),
                PayDirectionEnum.PAYIN.code()
        );
    }

    @Override
    public PspRouteResult selectPayout(PspOrderRequest order) {
        return select(
                order.getTenantId(),
                order.getMerchantId(),
                order.getMerchantAppId(),
                order.getCountryCode(),
                order.getCurrency(),
                order.getMethodCode(),
                order.getPayeeBankCode(),
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
            String bankCode,
            java.math.BigDecimal amount,
            String direction
    ) {
        String normalizedCountryCode = normalize(countryCode);
        String normalizedCurrency = normalize(currency);
        String normalizedMethodCode = normalize(methodCode);
        String normalizedBankCode = normalize(bankCode);
        String normalizedDirection = normalize(direction);
        if (requiresBankMapping(normalizedDirection, normalizedMethodCode) && StringUtils.isBlank(normalizedBankCode)) {
            throw new GkException(ErrorCode.BAD_REQUEST, "payee.bank_code is required for BANK_CARD payout");
        }
        PspRouteRuleEntity rule = pspRouteRuleDao.selectBestRouteRuleForOrder(
                tenantId,
                merchantId,
                merchantAppId,
                normalizedCountryCode,
                normalizedCurrency,
                normalizedMethodCode,
                normalizedBankCode,
                amount,
                normalizedDirection,
                LocalTime.now(),
                StatusEnum.NORMAL.code()
        );
        if (rule == null) {
            throw new GkException(ErrorCode.NOT_ACCEPTABLE, "No available PSP route");
        }
        PspProviderEntity provider = requireProvider(rule.getPspId(), normalizedDirection);
        PspMethodEntity method = requireMethod(rule.getPspMethodId());
        PspAccountEntity account = requirePspAccount(rule.getPspAccountId());
        if (!routeMethodMatches(normalizedCountryCode, normalizedCurrency, normalizedMethodCode, normalizedDirection, method)) {
            throw new GkException(ErrorCode.NOT_ACCEPTABLE, "PSP method does not match request method");
        }
        PspBankMappingEntity bankMapping = bankMapping(normalizedCountryCode, normalizedCurrency, normalizedDirection, normalizedMethodCode, normalizedBankCode, rule.getPspId());

        PspRouteResult result = new PspRouteResult();
        result.setRouteRuleId(rule.getId());
        result.setPspId(provider.getId());
        result.setPspCode(provider.getPspCode());
        result.setPspBaseUrl(provider.getBaseUrl());
        result.setProviderConfigJson(provider.getConfigJson());
        result.setPspCallbackUrl(platformCallbackUrl(account.getPspAccountNo(), normalizedDirection));
        result.setPspMethodId(method.getId());
        result.setPspMethodCode(method.getPspMethodCode());
        result.setMethodConfigJson(method.getConfigJson());
        result.setPspAccountId(account.getId());
        result.setPspAccountNo(account.getPspAccountNo());
        result.setPspAccountApiKey(account.getApiKey());
        result.setPspAccountApiSecret(account.getApiSecret());
        result.setAccountConfigJson(account.getConfigJson());
        result.setPspBankCode(bankMapping == null ? null : bankMapping.getPspBankCode());
        return result;
    }

    private PspBankMappingEntity bankMapping(String countryCode,
                                             String currency,
                                             String direction,
                                             String methodCode,
                                             String bankCode,
                                             Long pspId) {
        if (!requiresBankMapping(direction, methodCode)) {
            return null;
        }
        PspBankMappingEntity mapping = pspBankMappingDao.selectOne(new QueryWrapper<PspBankMappingEntity>()
                .eq("psp_id", pspId)
                .eq("country_code", normalize(countryCode))
                .eq("currency", normalize(currency))
                .eq("bank_code", normalize(bankCode))
                .eq("status", StatusEnum.NORMAL.code())
                .last("limit 1"));
        if (mapping == null || StringUtils.isBlank(mapping.getPspBankCode())) {
            throw new GkException(ErrorCode.NOT_ACCEPTABLE, "PSP bank mapping is not configured");
        }
        return mapping;
    }

    private boolean requiresBankMapping(String direction, String methodCode) {
        return PayDirectionEnum.PAYOUT.code().equals(direction)
                && isBankCardMethod(methodCode);
    }


    private boolean isBankCardMethod(String methodCode) {
        return "BANK_CARD".equalsIgnoreCase(StringUtils.trim(methodCode));
    }
    private String platformCallbackUrl(String pspAccountNo, String direction) {
        if (PayDirectionEnum.PAYOUT.code().equals(direction)) {
            return callbackUrlBuilder.payoutCallbackUrl(pspAccountNo);
        }
        return callbackUrlBuilder.payinCallbackUrl(pspAccountNo);
    }

    private PspProviderEntity requireProvider(Long pspId, String direction) {
        PspProviderEntity provider = cached(providerCache, pspId, pspProviderDao::selectById);
        if (provider == null || !StatusEnum.NORMAL.code().equals(provider.getStatus())) {
            throw new GkException(ErrorCode.NOT_ACCEPTABLE, "PSP provider is not available");
        }
        boolean supported = PayDirectionEnum.PAYOUT.code().equals(direction)
                ? Integer.valueOf(1).equals(provider.getSupportPayout())
                : Integer.valueOf(1).equals(provider.getSupportPayin());
        if (!supported) {
            throw new GkException(ErrorCode.NOT_ACCEPTABLE, "PSP provider is not available");
        }
        return provider;
    }

    private PspMethodEntity requireMethod(Long pspMethodId) {
        PspMethodEntity method = cached(methodCache, pspMethodId, pspMethodDao::selectById);
        if (method == null || !StatusEnum.NORMAL.code().equals(method.getStatus())) {
            throw new GkException(ErrorCode.NOT_ACCEPTABLE, "PSP method is not available");
        }
        return method;
    }

    private PspAccountEntity requirePspAccount(Long pspAccountId) {
        PspAccountEntity account = cached(accountCache, pspAccountId, pspAccountDao::selectById);
        if (account == null || !StatusEnum.NORMAL.code().equals(account.getStatus())) {
            throw new GkException(ErrorCode.NOT_ACCEPTABLE, "PSP account is not available");
        }
        return account;
    }

    private boolean routeMethodMatches(String countryCode, String currency, String methodCode, String direction, PspMethodEntity method) {
        if (method == null) {
            return false;
        }
        if (!Strings.CI.equals(StringUtils.trim(methodCode), StringUtils.trim(method.getMethodCode()))) {
            return false;
        }
        if (!Strings.CI.equals(StringUtils.trim(direction), StringUtils.trim(method.getDirection()))) {
            return false;
        }
        if (!Strings.CI.equals(StringUtils.trim(currency), StringUtils.trim(method.getCurrency()))) {
            return false;
        }
        return StringUtils.isBlank(countryCode)
                || StringUtils.isBlank(method.getCountryCode())
                || Strings.CI.equals(StringUtils.trim(countryCode), StringUtils.trim(method.getCountryCode()));
    }

    private String normalize(String value) {
        return StringUtils.defaultString(value).trim().toUpperCase(Locale.ROOT);
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
