package com.gk.ledger.service.impl;

import com.gk.ledger.service.LedgerAccountService;
import com.gk.merchant.service.MerchantLedgerAccountProvisioner;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MerchantLedgerAccountProvisionerImpl implements MerchantLedgerAccountProvisioner {

    private final LedgerAccountService ledgerAccountService;

    @Override
    public void provisionMerchantAccounts(Long tenantId, Long merchantId, String currency) {
        ledgerAccountService.provisionMerchantAccounts(tenantId, merchantId, currency);
    }
}
