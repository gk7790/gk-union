package com.gk.ledger.service;

import com.gk.common.core.service.CrudService;
import com.gk.ledger.dto.LedgerAccountDTO;
import com.gk.ledger.entity.LedgerAccountEntity;

public interface LedgerAccountService extends CrudService<LedgerAccountEntity, LedgerAccountDTO> {

    /**
     * 商户开户：创建默认可用/冻结账户及余额行。
     */
    void provisionMerchantAccounts(Long tenantId, Long merchantId, String currency);

    /**
     * 获取商户账户；不存在时幂等创建。
     */
    LedgerAccountEntity requireMerchantAccount(Long tenantId, Long merchantId, String accountType, String currency);
}
