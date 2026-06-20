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

    /**
     * PSP账户初始化：创建默认清算账户及余额行。
     */
    void provisionPspAccounts(Long tenantId, Long pspAccountId, String currency);

    /**
     * 获取PSP账户；不存在时幂等创建。
     */
    LedgerAccountEntity requirePspAccount(Long tenantId, Long pspAccountId, String accountType, String currency);

    /**
     * 租户级账户初始化：创建内部清算、内部手续费收入账户及余额行。
     */
    void provisionTenantAccounts(Long tenantId, String currency);

    /**
     * 获取内部户账户；不存在时幂等创建。
     */
    LedgerAccountEntity requireInternalAccount(Long tenantId, String accountType, String currency);

    /**
     * 获取平台级账户；不存在时幂等创建。
     */
    LedgerAccountEntity requirePlatformAccount(Long tenantId, String accountType, String currency);
}
