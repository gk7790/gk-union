package com.gk.psp.config;

import com.gk.common.constant.Constant;
import com.gk.infra.config.model.DomainConfig;
import com.gk.infra.config.service.SysParamConfigReader;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PspConfigService {
    private final SysParamConfigReader configReader;
    private final Environment environment;

    public PspCallbackConfig callback() {
        PspCallbackConfig fallback = new PspCallbackConfig();
        fallback.setBaseUrl(environment.getProperty("gk.psp.callback.base-url"));
        return configReader.getObject(Constant.PSP_CALLBACK_CONFIG_KEY, PspCallbackConfig.class, fallback);
    }

    public PspBalanceConfig balance() {
        PspBalanceConfig fallback = new PspBalanceConfig();
        PspBalanceConfig config = configReader.getObject(
                Constant.PSP_BALANCE_CONFIG_KEY, PspBalanceConfig.class, fallback);
        if (config.getCacheSeconds() <= 0) config.setCacheSeconds(fallback.getCacheSeconds());
        return config;
    }

    public DomainConfig domains() {
        return configReader.getObject(Constant.DOMAIN_CONFIG_KEY, DomainConfig.class, new DomainConfig());
    }
}
