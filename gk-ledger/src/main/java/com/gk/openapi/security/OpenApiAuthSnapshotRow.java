package com.gk.openapi.security;

import lombok.Data;

@Data
public class OpenApiAuthSnapshotRow {
    private Long tenantId;
    private Long merchantId;
    private Long merchantAppId;

    private String appId;
    private String apiSecret;
    private String signType;
    private Integer appStatus;
    private Integer nonceTtlSeconds;
    private Integer rateLimitQps;
    private String appEnv;
    private String allowedCurrencyJson;
    private String allowedMethodJson;

    private String merchantNo;
    private Integer merchantStatus;
    private String riskStatus;
    private String defaultCurrency;
    private String countryCode;
}
