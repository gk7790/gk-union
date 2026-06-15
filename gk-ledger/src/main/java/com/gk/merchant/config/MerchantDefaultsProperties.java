package com.gk.merchant.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "gk.merchant.defaults")
public class MerchantDefaultsProperties {
    private String timezone = "Asia/Shanghai";
    private String lang = "zh-CN";
    private String configJson = "{}";
}
