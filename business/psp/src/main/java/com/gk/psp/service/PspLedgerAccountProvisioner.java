package com.gk.psp.service;

public interface PspLedgerAccountProvisioner {

    void provisionPspAccounts(Long tenantId, Long pspAccountId, String currency);
}
