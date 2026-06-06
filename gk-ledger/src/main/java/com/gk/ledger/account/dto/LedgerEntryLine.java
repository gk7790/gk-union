package com.gk.ledger.account.dto;

import java.math.BigDecimal;

public record LedgerEntryLine(
        Long accountId,
        String direction,
        BigDecimal amount,
        String summary
) {
}
