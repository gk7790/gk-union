package com.gk.ledger.service;

import com.gk.common.core.service.CrudService;
import com.gk.ledger.dto.LedgerAccountDTO;
import com.gk.ledger.entity.LedgerAccountEntity;

public interface LedgerAccountService extends CrudService<LedgerAccountEntity, LedgerAccountDTO> {

    /**
     * Creates default merchant available, pending-settle and frozen accounts.
     */
    void provisionMerchantAccounts(Long tenantId, Long merchantId, String currency);

    /**
     * Returns a merchant account, creating it idempotently when missing.
     */
    LedgerAccountEntity requireMerchantAccount(Long tenantId, Long merchantId, String accountType, String currency);

    /**
     * Creates default PSP clearing accounts.
     */
    void provisionPspAccounts(Long tenantId, Long pspAccountId, String currency);

    /**
     * Returns a PSP account, creating it idempotently when missing.
     */
    LedgerAccountEntity requirePspAccount(Long tenantId, Long pspAccountId, String accountType, String currency);

    /**
     * Creates default tenant internal clearing and fee-income accounts.
     */
    void provisionTenantAccounts(Long tenantId, String currency);

    /**
     * Returns an internal tenant account, creating it idempotently when missing.
     */
    LedgerAccountEntity requireInternalAccount(Long tenantId, String accountType, String currency);

    /**
     * Returns a platform-level account, creating it idempotently when missing.
     */
    LedgerAccountEntity requirePlatformAccount(Long tenantId, String accountType, String currency);
}
