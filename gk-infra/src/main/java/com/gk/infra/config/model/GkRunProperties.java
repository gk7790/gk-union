package com.gk.infra.config.model;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "gk")
public class GkRunProperties {
    private Openapi openapi = new Openapi();
    private Psp psp = new Psp();
    private Merchant merchant = new Merchant();

    @Data
    public static class Openapi {
        private Payout payout = new Payout();
    }

    @Data
    public static class Payout {
        private boolean asyncSubmit = true;
    }

    @Data
    public static class Psp {
        private Callback callback = new Callback();
    }

    @Data
    public static class Callback {
        private String baseUrl;
    }

    @Data
    public static class Merchant {
        private Defaults defaults = new Defaults();
    }

    @Data
    public static class Defaults {
        private String timezone = "UTC";
        private String lang = "zh-CN";
        private String countryCode = "PH";
        private String configJson = "{}";
    }
}
