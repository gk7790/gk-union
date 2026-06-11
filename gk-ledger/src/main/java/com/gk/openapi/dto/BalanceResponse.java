package com.gk.openapi.dto;

import lombok.Data;

@Data
@OpenApiModel
public class BalanceResponse {
    private String accountNo;
    private String currency;
    private String balance;
}
