package com.gk.openapi.service;

import com.gk.openapi.dto.BalanceResponse;

import java.util.List;

public interface OpenBalanceService {
    List<BalanceResponse> list(String currency);
}
