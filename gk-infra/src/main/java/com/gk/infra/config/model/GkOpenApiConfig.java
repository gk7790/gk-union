package com.gk.infra.config.model;

import lombok.Data;

@Data
public class GkOpenApiConfig {
    private boolean requireNonce = true;
    private String defaultSignType = "HMAC_SHA256";
    private String sandboxPayUrl;
}
