package com.gk.psp.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "gk.psp.callback")
public class PspCallbackProperties {
    /**
     * Public platform base URL exposed to PSP, for example {@code https://api.example.com}.
     */
    private String baseUrl;
}
