package com.gk.infra.config.model;

import lombok.Data;

@Data
public class DomainConfig {
    private String apiBaseUrl;
    private String adminBaseUrl;
    private String cashierBaseUrl;
    private String merchantPortalBaseUrl;
    private String deeplinkBaseUrl;
}
