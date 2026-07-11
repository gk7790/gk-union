package com.gk.merchant.config;

import lombok.Data;

@Data
public class MerchantApiConfig {
    private String defaultSignType = "HMAC_SHA256";
}
