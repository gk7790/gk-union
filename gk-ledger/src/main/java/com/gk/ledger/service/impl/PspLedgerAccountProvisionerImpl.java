package com.gk.ledger.service.impl;

import com.gk.ledger.service.LedgerAccountService;
import com.gk.psp.service.PspLedgerAccountProvisioner;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PspLedgerAccountProvisionerImpl implements PspLedgerAccountProvisioner {

    private final LedgerAccountService ledgerAccountService;

    @Override
    public void provisionPspAccounts(Long tenantId, Long pspAccountId, String currency) {
        ledgerAccountService.provisionPspAccounts(tenantId, pspAccountId, currency);
    }
}
