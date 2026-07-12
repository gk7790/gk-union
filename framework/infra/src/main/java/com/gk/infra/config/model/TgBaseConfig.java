package com.gk.infra.config.model;

import lombok.Data;

@Data
public class TgBaseConfig {
    private String apiBaseUrl = "https://api.telegram.org";
    private String webhookBaseUrl = "";
    private Integer bindCodeTtl = 10;
}
