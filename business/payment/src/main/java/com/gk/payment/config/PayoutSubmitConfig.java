package com.gk.payment.config;

import lombok.Data;

@Data
public class PayoutSubmitConfig {
    private boolean asyncSubmit = true;
    private int defaultBatchSize = 20;
    private long firstQueryDelaySeconds = 60;
    private int maxRouteAttempts = 3;
}
