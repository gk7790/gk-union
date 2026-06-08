package com.gk.openapi.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class BalanceResponse {
    private String accountNo;
    private String accountType;
    private String currency;
    private BigDecimal balance;
    private BigDecimal debitTotal;
    private BigDecimal creditTotal;
}
