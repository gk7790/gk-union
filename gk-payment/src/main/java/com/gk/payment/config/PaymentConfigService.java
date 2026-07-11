package com.gk.payment.config;

import com.gk.common.constant.Constant;
import com.gk.infra.config.service.SysParamConfigReader;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class PaymentConfigService {
    private final SysParamConfigReader configReader;
    private final Environment environment;

    public MerchantNotifyConfig merchantNotify() {
        MerchantNotifyConfig fallback = new MerchantNotifyConfig();
        MerchantNotifyConfig config = configReader.getObject(
                Constant.MERCHANT_NOTIFY_CONFIG_KEY, MerchantNotifyConfig.class, fallback);
        if (config.getConnectTimeoutMs() <= 0) config.setConnectTimeoutMs(fallback.getConnectTimeoutMs());
        if (config.getReadTimeoutMs() <= 0) config.setReadTimeoutMs(fallback.getReadTimeoutMs());
        if (config.getMaxRetryCount() <= 0) config.setMaxRetryCount(fallback.getMaxRetryCount());
        config.setSuccessTokens(normalizeTokens(config.getSuccessTokens(), fallback.getSuccessTokens()));
        return config;
    }

    public PspQueryConfig pspQuery() {
        PspQueryConfig fallback = new PspQueryConfig();
        PspQueryConfig config = configReader.getObject(
                Constant.PSP_QUERY_CONFIG_KEY, PspQueryConfig.class, fallback);
        if (config.getMaxQueryCount() <= 0) config.setMaxQueryCount(fallback.getMaxQueryCount());
        if (config.getBackoffSeconds() == null || config.getBackoffSeconds().isEmpty()) {
            config.setBackoffSeconds(fallback.getBackoffSeconds());
        }
        return config;
    }

    public PayoutSubmitConfig payoutSubmit() {
        PayoutSubmitConfig fallback = new PayoutSubmitConfig();
        fallback.setAsyncSubmit(environment.getProperty("gk.openapi.payout.async-submit", Boolean.class, true));
        PayoutSubmitConfig config = configReader.getObject(
                Constant.PAYOUT_SUBMIT_CONFIG_KEY, PayoutSubmitConfig.class, fallback);
        if (config.getDefaultBatchSize() <= 0) config.setDefaultBatchSize(fallback.getDefaultBatchSize());
        if (config.getFirstQueryDelaySeconds() <= 0) config.setFirstQueryDelaySeconds(fallback.getFirstQueryDelaySeconds());
        if (config.getMaxRouteAttempts() <= 0) config.setMaxRouteAttempts(fallback.getMaxRouteAttempts());
        return config;
    }

    private List<String> normalizeTokens(List<String> tokens, List<String> fallback) {
        List<String> normalized = tokens == null ? List.of() : tokens.stream()
                .map(String::trim).filter(value -> !value.isEmpty())
                .map(value -> value.toLowerCase(Locale.ROOT)).distinct().toList();
        return normalized.isEmpty() ? fallback : normalized;
    }
}
