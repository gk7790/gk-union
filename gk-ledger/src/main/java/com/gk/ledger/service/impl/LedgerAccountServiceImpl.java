package com.gk.ledger.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gk.common.core.service.impl.CrudServiceImpl;
import com.gk.common.enums.SubjectTypeEnum;
import com.gk.common.model.DynMap;
import com.gk.infra.enums.StatusEnum;
import com.gk.ledger.dao.LedgerAccountDao;
import com.gk.ledger.dao.LedgerBalanceDao;
import com.gk.ledger.dto.LedgerAccountDTO;
import com.gk.ledger.enums.LedgerAccountTypeEnum;
import com.gk.ledger.enums.LedgerDirectionEnum;
import com.gk.ledger.enums.LedgerOwnerTypeEnum;
import com.gk.ledger.entity.LedgerAccountEntity;
import com.gk.ledger.entity.LedgerBalanceEntity;
import com.gk.ledger.service.LedgerAccountService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class LedgerAccountServiceImpl extends CrudServiceImpl<LedgerAccountDao, LedgerAccountEntity, LedgerAccountDTO> implements LedgerAccountService {

    private static final int MONEY_SCALE = 8;

    private final LedgerBalanceDao ledgerBalanceDao;

    @Override
    public QueryWrapper<LedgerAccountEntity> getWrapper(DynMap params) {
        QueryWrapper<LedgerAccountEntity> wrapper = new QueryWrapper<>();
        Long tenantId = params.getLong("tenantId", null);
        Long ownerId = params.getLong("ownerId", null);
        Integer status = params.containsKey("status") ? params.getInt("status") : null;
        Integer allowNegative = params.containsKey("allowNegative") ? params.getInt("allowNegative") : null;
        String accountNo = params.getStr("accountNo");
        String ownerType = params.getStr("ownerType");
        String accountType = params.getStr("accountType");
        String currency = params.getStr("currency");
        String normalSide = params.getStr("normalSide");

        wrapper.eq(tenantId != null, "tenant_id", tenantId);
        wrapper.eq(ownerId != null, "owner_id", ownerId);
        wrapper.eq(status != null, "status", status);
        wrapper.eq(allowNegative != null, "allow_negative", allowNegative);
        wrapper.eq(StrUtil.isNotBlank(accountNo), "account_no", accountNo);
        wrapper.eq(StrUtil.isNotBlank(ownerType), "owner_type", ownerType);
        wrapper.eq(StrUtil.isNotBlank(accountType), "account_type", accountType);
        wrapper.eq(StrUtil.isNotBlank(currency), "currency", currency);
        wrapper.eq(StrUtil.isNotBlank(normalSide), "normal_side", normalSide);
        return wrapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void provisionMerchantAccounts(Long tenantId, Long merchantId, String currency) {
        String normalizedCurrency = normalizeCurrency(currency);
        requireMerchantAccount(tenantId, merchantId, LedgerAccountTypeEnum.MERCHANT_AVAILABLE.code(), normalizedCurrency);
        requireMerchantAccount(tenantId, merchantId, LedgerAccountTypeEnum.MERCHANT_PENDING_SETTLE.code(), normalizedCurrency);
        requireMerchantAccount(tenantId, merchantId, LedgerAccountTypeEnum.MERCHANT_FROZEN.code(), normalizedCurrency);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LedgerAccountEntity requireMerchantAccount(Long tenantId, Long merchantId, String accountType, String currency) {
        String normalizedCurrency = normalizeCurrency(currency);
        LedgerAccountEntity account = findMerchantAccount(tenantId, merchantId, accountType, normalizedCurrency);
        if (account != null) {
            ensureBalance(account);
            return account;
        }
        account = new LedgerAccountEntity();
        account.setTenantId(tenantId);
        account.setOwnerType(SubjectTypeEnum.MERCHANT.code());
        account.setOwnerId(merchantId);
        account.setAccountType(accountType);
        account.setCurrency(normalizedCurrency);
        account.setAccountNo(buildMerchantAccountNo(tenantId, merchantId, accountType, normalizedCurrency));
        account.setNormalSide(LedgerDirectionEnum.CREDIT.code());
        account.setAllowNegative(0);
        account.setStatus(StatusEnum.NORMAL.code());
        try {
            baseDao.insert(account);
        } catch (DuplicateKeyException ex) {
            account = findMerchantAccount(tenantId, merchantId, accountType, normalizedCurrency);
            if (account == null) {
                throw ex;
            }
        }
        ensureBalance(account);
        return account;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void provisionPspAccounts(Long tenantId, Long pspAccountId, String currency) {
        String normalizedCurrency = normalizeCurrency(currency);
        requirePspAccount(tenantId, pspAccountId, LedgerAccountTypeEnum.PSP_CLEARING.code(), normalizedCurrency);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LedgerAccountEntity requirePspAccount(Long tenantId, Long pspAccountId, String accountType, String currency) {
        String normalizedCurrency = normalizeCurrency(currency);
        LedgerAccountEntity account = findPspAccount(tenantId, pspAccountId, accountType, normalizedCurrency);
        if (account != null) {
            ensureBalance(account);
            return account;
        }
        account = new LedgerAccountEntity();
        account.setTenantId(tenantId);
        account.setOwnerType(LedgerOwnerTypeEnum.PSP.code());
        account.setOwnerId(pspAccountId);
        account.setAccountType(accountType);
        account.setCurrency(normalizedCurrency);
        account.setAccountNo(buildPspAccountNo(tenantId, pspAccountId, accountType, normalizedCurrency));
        account.setNormalSide(LedgerDirectionEnum.DEBIT.code());
        account.setAllowNegative(1);
        account.setStatus(StatusEnum.NORMAL.code());
        try {
            baseDao.insert(account);
        } catch (DuplicateKeyException ex) {
            account = findPspAccount(tenantId, pspAccountId, accountType, normalizedCurrency);
            if (account == null) {
                throw ex;
            }
        }
        ensureBalance(account);
        return account;
    }

    private LedgerAccountEntity findMerchantAccount(Long tenantId, Long merchantId, String accountType, String currency) {
        return baseDao.selectOne(new QueryWrapper<LedgerAccountEntity>()
                .eq("tenant_id", tenantId)
                .eq("owner_type", SubjectTypeEnum.MERCHANT.code())
                .eq("owner_id", merchantId)
                .eq("account_type", accountType)
                .eq("currency", currency)
                .eq("status", StatusEnum.NORMAL.code())
                .last("limit 1"));
    }

    private LedgerAccountEntity findPspAccount(Long tenantId, Long pspAccountId, String accountType, String currency) {
        return baseDao.selectOne(new QueryWrapper<LedgerAccountEntity>()
                .eq("tenant_id", tenantId)
                .eq("owner_type", LedgerOwnerTypeEnum.PSP.code())
                .eq("owner_id", pspAccountId)
                .eq("account_type", accountType)
                .eq("currency", currency)
                .eq("status", StatusEnum.NORMAL.code())
                .last("limit 1"));
    }

    private void ensureBalance(LedgerAccountEntity account) {
        LedgerBalanceEntity existed = ledgerBalanceDao.selectOne(new QueryWrapper<LedgerBalanceEntity>()
                .eq("tenant_id", account.getTenantId())
                .eq("account_id", account.getId())
                .last("limit 1"));
        if (existed != null) {
            return;
        }
        LedgerBalanceEntity balance = new LedgerBalanceEntity();
        balance.setTenantId(account.getTenantId());
        balance.setAccountId(account.getId());
        balance.setAccountNo(account.getAccountNo());
        balance.setCurrency(account.getCurrency());
        balance.setBalance(zeroAmount());
        balance.setDebitTotal(zeroAmount());
        balance.setCreditTotal(zeroAmount());
        balance.setVersion(0);
        try {
            ledgerBalanceDao.insert(balance);
        } catch (DuplicateKeyException ignored) {
            // concurrent init
        }
    }

    static String buildMerchantAccountNo(Long tenantId, Long merchantId, String accountType, String currency) {
        return "T" + tenantId + "-M" + merchantId + "-" + accountTypeToken(accountType) + "-" + currency;
    }

    static String buildPspAccountNo(Long tenantId, Long pspAccountId, String accountType, String currency) {
        return "T" + tenantId + "-PSP" + pspAccountId + "-" + accountTypeToken(accountType) + "-" + currency;
    }

    private static String accountTypeToken(String accountType) {
        if (LedgerAccountTypeEnum.PSP_CLEARING.matches(accountType)) {
            return "CLR";
        }
        if (LedgerAccountTypeEnum.MERCHANT_FROZEN.matches(accountType)) {
            return "FRZ";
        }
        if (LedgerAccountTypeEnum.MERCHANT_PENDING_SETTLE.matches(accountType)) {
            return "PND";
        }
        return "AVL";
    }

    private static String normalizeCurrency(String currency) {
        return StringUtils.defaultString(currency).trim().toUpperCase(Locale.ROOT);
    }

    private static BigDecimal zeroAmount() {
        return BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }
}
