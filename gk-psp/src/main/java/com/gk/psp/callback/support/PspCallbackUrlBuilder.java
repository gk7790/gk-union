package com.gk.psp.callback.support;

import com.gk.infra.config.service.GkSysParamsConfigService;
import com.gk.psp.config.PspCallbackProperties;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PspCallbackUrlBuilder {
    private static final String CALLBACK_ROOT = "/psp/callback";

    private final PspCallbackProperties properties;
    private final GkSysParamsConfigService configService;

    public String payCallbackUrl(String pspCode) {
        return callbackUrl(pspCode, "pay");
    }

    public String payoutCallbackUrl(String pspCode) {
        return callbackUrl(pspCode, "payout");
    }

    private String callbackUrl(String pspCode, String callbackType) {
        if (StringUtils.isBlank(pspCode)) {
            throw new IllegalArgumentException("pspCode is required");
        }
        String baseUrl = StringUtils.trimToNull(configService.pspCallbackConfig().getBaseUrl());
        if (baseUrl == null) {
            baseUrl = StringUtils.trimToNull(properties.getBaseUrl());
        }
        if (baseUrl == null) {
            throw new IllegalStateException("PSP callback base url is not configured");
        }
        return removeTrailingSlash(baseUrl)
                + CALLBACK_ROOT
                + "/"
                + StringUtils.trim(pspCode)
                + "/"
                + callbackType;
    }

    private String removeTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
