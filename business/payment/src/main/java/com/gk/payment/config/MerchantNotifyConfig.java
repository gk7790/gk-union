package com.gk.payment.config;

import lombok.Data;

import java.util.List;

@Data
public class MerchantNotifyConfig {
    private int connectTimeoutMs = 3000;
    private int readTimeoutMs = 5000;
    private List<String> successTokens = List.of("success", "ok");
    private int maxRetryCount = 16;
}
