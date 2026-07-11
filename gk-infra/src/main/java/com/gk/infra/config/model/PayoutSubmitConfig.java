package com.gk.infra.config.model;

import lombok.Data;

@Data
public class PayoutSubmitConfig {
    private boolean asyncSubmit = true;
    private int defaultBatchSize = 20;
    private long firstQueryDelaySeconds = 60;
    private int maxRouteAttempts = 3;
}
