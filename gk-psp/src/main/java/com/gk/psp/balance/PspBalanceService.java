package com.gk.psp.balance;

import java.math.BigDecimal;

public interface PspBalanceService {
    PspBalanceSnap getCached(Long tenantId, Long pspAccountId);

    PspBalanceSnap refresh(Long pspAccountId);

    int refreshAll();

    boolean isPayoutBalanceAvailable(Long tenantId, Long pspAccountId, String currency, BigDecimal amount);
}
