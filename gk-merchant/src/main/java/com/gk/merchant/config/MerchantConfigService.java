package com.gk.merchant.config;

import com.gk.common.constant.Constant;
import com.gk.payment.domain.enums.SignTypeEnum;
import com.gk.infra.config.service.SysParamConfigReader;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MerchantConfigService {
    private final SysParamConfigReader configReader;
    private final Environment environment;

    public MerchantDefaultConfig defaults() {
        MerchantDefaultConfig fallback = new MerchantDefaultConfig();
        fallback.setTimezone(environment.getProperty("gk.merchant.defaults.timezone", "UTC"));
        fallback.setLang(environment.getProperty("gk.merchant.defaults.lang", "zh-CN"));
        fallback.setCountryCode(environment.getProperty("gk.merchant.defaults.country-code", "PH"));
        fallback.setConfigJson(environment.getProperty("gk.merchant.defaults.config-json", "{}"));
        MerchantDefaultConfig config = configReader.getObject(
                Constant.MERCHANT_DEFAULT_CONFIG_KEY, MerchantDefaultConfig.class, fallback);
        config.setTimezone(StringUtils.defaultIfBlank(config.getTimezone(), fallback.getTimezone()));
        config.setLang(StringUtils.defaultIfBlank(config.getLang(), fallback.getLang()));
        config.setCountryCode(StringUtils.defaultIfBlank(config.getCountryCode(), fallback.getCountryCode()));
        if (config.getConfigJson() == null) {
            config.setConfigJson(fallback.getConfigJson());
        }
        return config;
    }

    public String defaultApiSignType() {
        MerchantApiConfig config = configReader.getObject(
                Constant.GK_OPENAPI_CONFIG_KEY, MerchantApiConfig.class, new MerchantApiConfig());
        return SignTypeEnum.normalizeOpenApiSignType(config.getDefaultSignType());
    }
}
