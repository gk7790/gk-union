package com.gk.psp.callback.support;

import com.gk.psp.config.PspConfigService;
import com.gk.psp.config.PspCallbackProperties;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PspCallbackUrlBuilder {
    private static final String CALLBACK_ROOT = "/psp/callback";

    private final PspCallbackProperties properties;
    private final PspConfigService configService;

    public String payinCallbackUrl(String pspAccountNo) {
        return callbackUrl(pspAccountNo, "payin");
    }

    public String payoutCallbackUrl(String pspAccountNo) {
        return callbackUrl(pspAccountNo, "payout");
    }

    private String callbackUrl(String pspAccountNo, String callbackType) {
        if (StringUtils.isBlank(pspAccountNo)) {
            throw new IllegalArgumentException("pspAccountNo is required");
        }
        String baseUrl = StringUtils.trimToNull(configService.callback().getBaseUrl());
        if (baseUrl == null) {
            baseUrl = StringUtils.trimToNull(properties.getBaseUrl());
        }
        if (baseUrl == null) {
            throw new IllegalStateException("PSP callback base url is not configured");
        }
        return removeTrailingSlash(baseUrl)
                + CALLBACK_ROOT
                + "/"
                + StringUtils.trim(pspAccountNo)
                + "/"
                + callbackType;
    }

    private String removeTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
