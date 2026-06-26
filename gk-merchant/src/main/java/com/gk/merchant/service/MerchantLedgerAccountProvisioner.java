package com.gk.merchant.service;

public interface MerchantLedgerAccountProvisioner {

    void provisionMerchantAccounts(Long tenantId, Long merchantId, String currency);
}
