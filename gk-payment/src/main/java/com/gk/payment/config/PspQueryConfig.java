package com.gk.payment.config;

import lombok.Data;

import java.util.List;

@Data
public class PspQueryConfig {
    private int maxQueryCount = 30;
    private List<Long> backoffSeconds = List.of(60L, 120L, 300L, 600L, 900L, 1800L);
}
