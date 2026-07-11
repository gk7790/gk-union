package com.gk.infra.config.model;

import com.alibaba.fastjson2.JSON;
import lombok.Data;

@Data
public class MerchantDefaultConfig {
    private String timezone = "UTC";
    private String lang = "zh-CN";
    private String countryCode = "PH";
    private Object configJson = "{}";

    public String configJsonText() {
        if (configJson == null) {
            return "{}";
        }
        if (configJson instanceof String text) {
            return text.isBlank() ? "{}" : text;
        }
        return JSON.toJSONString(configJson);
    }
}
