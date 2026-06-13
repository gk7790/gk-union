package com.gk.ledger.service.impl;

import com.gk.ledger.enums.LedgerAccountTypeEnum;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LedgerAccountServiceImplTest {
    @Test
    void buildMerchantAccountNoUsesDistinctTokensForMerchantAccountTypes() {
        Long tenantId = 10L;
        Long merchantId = 20L;
        String currency = "USD";

        assertThat(LedgerAccountServiceImpl.buildMerchantAccountNo(
                tenantId, merchantId, LedgerAccountTypeEnum.MERCHANT_AVAILABLE.code(), currency
        )).isEqualTo("T10-M20-AVL-USD");
        assertThat(LedgerAccountServiceImpl.buildMerchantAccountNo(
                tenantId, merchantId, LedgerAccountTypeEnum.MERCHANT_PENDING_SETTLE.code(), currency
        )).isEqualTo("T10-M20-PND-USD");
        assertThat(LedgerAccountServiceImpl.buildMerchantAccountNo(
                tenantId, merchantId, LedgerAccountTypeEnum.MERCHANT_FROZEN.code(), currency
        )).isEqualTo("T10-M20-FRZ-USD");
    }
}
