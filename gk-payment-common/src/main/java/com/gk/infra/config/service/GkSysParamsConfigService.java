package com.gk.infra.config.service;

import com.alibaba.fastjson2.JSON;
import com.gk.common.constant.Constant;
import com.gk.common.config.SysParamReader;
import com.gk.common.enums.SignTypeEnum;
import com.gk.infra.config.model.DomainConfig;
import com.gk.infra.config.model.GkOpenApiConfig;
import com.gk.infra.config.model.GkRunProperties;
import com.gk.infra.config.model.MerchantDefaultConfig;
import com.gk.infra.config.model.MerchantNotifyConfig;
import com.gk.infra.config.model.PayoutSubmitConfig;
import com.gk.infra.config.model.PspBalanceConfig;
import com.gk.infra.config.model.PspCallbackConfig;
import com.gk.infra.config.model.PspQueryConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class GkSysParamsConfigService {
    private final SysParamReader sysParamsService;
    private final GkRunProperties properties;

    public GkOpenApiConfig openApiConfig() {
        GkOpenApiConfig fallback = new GkOpenApiConfig();
        GkOpenApiConfig config = getObject(Constant.GK_OPENAPI_CONFIG_KEY, GkOpenApiConfig.class, fallback);
        config.setDefaultSignType(SignTypeEnum.normalizeOpenApiSignType(config.getDefaultSignType()));
        return config;
    }

    public PspCallbackConfig pspCallbackConfig() {
        PspCallbackConfig fallback = new PspCallbackConfig();
        fallback.setBaseUrl(properties.getPsp().getCallback().getBaseUrl());
        return getObject(Constant.PSP_CALLBACK_CONFIG_KEY, PspCallbackConfig.class, fallback);
    }

    public MerchantDefaultConfig merchantDefaultConfig() {
        MerchantDefaultConfig fallback = new MerchantDefaultConfig();
        GkRunProperties.Defaults defaults = properties.getMerchant().getDefaults();
        fallback.setTimezone(defaults.getTimezone());
        fallback.setLang(defaults.getLang());
        fallback.setCountryCode(defaults.getCountryCode());
        fallback.setConfigJson(defaults.getConfigJson());
        MerchantDefaultConfig config = getObject(Constant.MERCHANT_DEFAULT_CONFIG_KEY, MerchantDefaultConfig.class, fallback);
        config.setTimezone(StringUtils.defaultIfBlank(config.getTimezone(), fallback.getTimezone()));
        config.setLang(StringUtils.defaultIfBlank(config.getLang(), fallback.getLang()));
        config.setCountryCode(StringUtils.defaultIfBlank(config.getCountryCode(), fallback.getCountryCode()));
        if (config.getConfigJson() == null) {
            config.setConfigJson(fallback.getConfigJson());
        }
        return config;
    }

    public MerchantNotifyConfig merchantNotifyConfig() {
        MerchantNotifyConfig fallback = new MerchantNotifyConfig();
        MerchantNotifyConfig config = getObject(Constant.MERCHANT_NOTIFY_CONFIG_KEY, MerchantNotifyConfig.class, fallback);
        if (config.getConnectTimeoutMs() <= 0) {
            config.setConnectTimeoutMs(fallback.getConnectTimeoutMs());
        }
        if (config.getReadTimeoutMs() <= 0) {
            config.setReadTimeoutMs(fallback.getReadTimeoutMs());
        }
        if (config.getMaxRetryCount() <= 0) {
            config.setMaxRetryCount(fallback.getMaxRetryCount());
        }
        config.setSuccessTokens(normalizeTokens(config.getSuccessTokens(), fallback.getSuccessTokens()));
        return config;
    }

    public PspQueryConfig pspQueryConfig() {
        PspQueryConfig fallback = new PspQueryConfig();
        PspQueryConfig config = getObject(Constant.PSP_QUERY_CONFIG_KEY, PspQueryConfig.class, fallback);
        if (config.getMaxQueryCount() <= 0) {
            config.setMaxQueryCount(fallback.getMaxQueryCount());
        }
        if (config.getBackoffSeconds() == null || config.getBackoffSeconds().isEmpty()) {
            config.setBackoffSeconds(fallback.getBackoffSeconds());
        }
        return config;
    }

    public PayoutSubmitConfig payoutSubmitConfig() {
        PayoutSubmitConfig fallback = new PayoutSubmitConfig();
        fallback.setAsyncSubmit(properties.getOpenapi().getPayout().isAsyncSubmit());
        PayoutSubmitConfig config = getObject(Constant.PAYOUT_SUBMIT_CONFIG_KEY, PayoutSubmitConfig.class, fallback);
        if (config.getDefaultBatchSize() <= 0) {
            config.setDefaultBatchSize(fallback.getDefaultBatchSize());
        }
        if (config.getFirstQueryDelaySeconds() <= 0) {
            config.setFirstQueryDelaySeconds(fallback.getFirstQueryDelaySeconds());
        }
        if (config.getMaxRouteAttempts() <= 0) {
            config.setMaxRouteAttempts(fallback.getMaxRouteAttempts());
        }
        return config;
    }

    public PspBalanceConfig pspBalanceConfig() {
        PspBalanceConfig fallback = new PspBalanceConfig();
        PspBalanceConfig config = getObject(Constant.PSP_BALANCE_CONFIG_KEY, PspBalanceConfig.class, fallback);
        if (config.getCacheSeconds() <= 0) {
            config.setCacheSeconds(fallback.getCacheSeconds());
        }
        return config;
    }

    public DomainConfig domainConfig() {
        DomainConfig fallback = new DomainConfig();
        return getObject(Constant.DOMAIN_CONFIG_KEY, DomainConfig.class, fallback);
    }

    private <T> T getObject(String key, Class<T> type, T fallback) {
        try {
            String value = sysParamsService.getValue(key);
            if (StringUtils.isBlank(value)) {
                return fallback;
            }
            T config = JSON.parseObject(value, type);
            return config == null ? fallback : config;
        } catch (Exception ex) {
            log.warn("Load sys params config failed, key={}, err={}", key, ex.getMessage());
            return fallback;
        }
    }

    private List<String> normalizeTokens(List<String> tokens, List<String> fallback) {
        List<String> normalized = tokens == null ? List.of() : tokens.stream()
                .map(StringUtils::trimToNull)
                .filter(StringUtils::isNotBlank)
                .map(token -> token.toLowerCase(Locale.ROOT))
                .distinct()
                .toList();
        return normalized.isEmpty() ? fallback : normalized;
    }
}
