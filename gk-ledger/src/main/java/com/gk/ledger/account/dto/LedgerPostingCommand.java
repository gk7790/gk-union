package com.gk.ledger.account.dto;

import java.math.BigDecimal;
import java.util.List;

public record LedgerPostingCommand(
        Long tenantId,
        Long merchantId,
        String journalNo,
        String bizType,
        String bizNo,
        String currency,
        BigDecimal totalAmount,
        String summary,
        List<LedgerEntryLine> entries
) {
}
