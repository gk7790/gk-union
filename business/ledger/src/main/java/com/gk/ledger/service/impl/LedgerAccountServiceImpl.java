package com.gk.ledger.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gk.common.context.ReqContext;
import com.gk.common.context.ReqContextHolder;
import com.gk.common.core.service.impl.CrudServiceImpl;
import com.gk.common.enums.SubjectTypeEnum;
import com.gk.common.model.DynMap;
import com.gk.common.model.PageData;
import com.gk.common.transaction.SavepointExecutor;
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
import com.gk.ledger.support.SubjectDisplayEnricher;
import com.gk.merchant.dao.MerchantDao;
import com.gk.merchant.entity.MerchantEntity;
import com.gk.psp.dao.PspAccountDao;
import com.gk.psp.entity.PspAccountEntity;
import com.gk.tenant.dao.TenantDao;
import com.gk.tenant.entity.TenantEntity;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class LedgerAccountServiceImpl extends CrudServiceImpl<LedgerAccountDao, LedgerAccountEntity, LedgerAccountDTO> implements LedgerAccountService {

    private static final int MONEY_SCALE = 8;

    private final LedgerBalanceDao ledgerBalanceDao;
    private final TenantDao tenantDao;
    private final MerchantDao merchantDao;
    private final PspAccountDao pspAccountDao;
    @Autowired(required = false)
    private SubjectDisplayEnricher subjectDisplayEnricher;

    @Override
    public PageData<LedgerAccountDTO> page(DynMap params) {
        PageData<LedgerAccountDTO> page = super.page(params);
        enrichAccounts(page.getItems());
        return page;
    }

    @Override
    public List<LedgerAccountDTO> list(DynMap params) {
        List<LedgerAccountDTO> items = super.list(params);
        enrichAccounts(items);
        return items;
    }

    @Override
    public LedgerAccountDTO get(Long id) {
        LedgerAccountDTO dto = super.get(id);
        if (dto != null) {
            enrichAccounts(List.of(dto));
        }
        return dto;
    }

    private void enrichAccounts(List<LedgerAccountDTO> items) {
        if (subjectDisplayEnricher != null) {
            subjectDisplayEnricher.enrichAccounts(items);
        }
    }

    @Override
    public QueryWrapper<LedgerAccountEntity> getWrapper(DynMap params) {
        QueryWrapper<LedgerAccountEntity> wrapper = new QueryWrapper<>();
        Long ownerId = params.getLong("ownerId", null);
        Integer status = params.containsKey("status") ? params.getInt("status") : null;
        Integer allowNegative = params.containsKey("allowNegative") ? params.getInt("allowNegative") : null;
        String accountNo = params.getStr("accountNo");
        String ownerType = params.getStr("ownerType");
        String accountType = params.getStr("accountType");
        String currency = params.getStr("currency");
        String normalSide = params.getStr("normalSide");
        ReqContext context = ReqContextHolder.get();

        if (SubjectTypeEnum.PLATFORM.code().equals(context.getSubjectType())) {
            Long tenantId = params.getLong("tenantId", 0L);
            Long merchantId = params.getLong("merchantId", 0L);
            wrapper.eq(tenantId > 0, "tenant_id", tenantId);
            if (merchantId > 0) {
                ownerId = merchantId;
                ownerType = LedgerOwnerTypeEnum.MERCHANT.code();
            }
        } else if (SubjectTypeEnum.TENANT.code().equals(context.getSubjectType())) {
            wrapper.eq("tenant_id", context.getTenantId());
            Long merchantId = params.getLong("merchantId", 0L);
            if (merchantId > 0) {
                ownerId = merchantId;
                ownerType = LedgerOwnerTypeEnum.MERCHANT.code();
            }
        } else {
            ownerType = LedgerOwnerTypeEnum.MERCHANT.code();
            ownerId = context.getMerchantId();
            wrapper.eq("tenant_id", context.getTenantId());
        }

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
        requireMerchantAccount(tenantId, merchantId, LedgerAccountTypeEnum.AVAILABLE.code(), normalizedCurrency);
        requireMerchantAccount(tenantId, merchantId, LedgerAccountTypeEnum.PENDING_SETTLE.code(), normalizedCurrency);
        requireMerchantAccount(tenantId, merchantId, LedgerAccountTypeEnum.FROZEN.code(), normalizedCurrency);
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
        account.setAccountNo(buildMerchantAccountNo(requireMerchantNo(merchantId), accountType, normalizedCurrency));
        account.setNormalSide(LedgerDirectionEnum.CREDIT.code());
        account.setAllowNegative(0);
        account.setStatus(StatusEnum.NORMAL.code());
        try {
            insertAccountWithSavepoint(account);
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
        requirePspAccount(tenantId, pspAccountId, LedgerAccountTypeEnum.CLEARING.code(), normalizedCurrency);
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
        account.setAccountNo(buildPspAccountNo(requirePspAccountNo(pspAccountId), accountType, normalizedCurrency));
        account.setNormalSide(LedgerDirectionEnum.DEBIT.code());
        account.setAllowNegative(1);
        account.setStatus(StatusEnum.NORMAL.code());
        try {
            insertAccountWithSavepoint(account);
        } catch (DuplicateKeyException ex) {
            account = findPspAccount(tenantId, pspAccountId, accountType, normalizedCurrency);
            if (account == null) {
                throw ex;
            }
        }
        ensureBalance(account);
        return account;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void provisionTenantAccounts(Long tenantId, String currency) {
        String normalizedCurrency = normalizeCurrency(currency);
        requireInternalAccount(tenantId, LedgerAccountTypeEnum.CLEARING.code(), normalizedCurrency);
        requireInternalAccount(tenantId, LedgerAccountTypeEnum.FEE_INCOME.code(), normalizedCurrency);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LedgerAccountEntity requireInternalAccount(Long tenantId, String accountType, String currency) {
        boolean feeIncome = LedgerAccountTypeEnum.FEE_INCOME.matches(accountType);
        return requireOwnerAccount(
                tenantId,
                LedgerOwnerTypeEnum.INTERNAL.code(),
                accountType,
                currency,
                buildInternalAccountNo(tenantId, accountType, currency),
                feeIncome ? LedgerDirectionEnum.CREDIT.code() : LedgerDirectionEnum.DEBIT.code(),
                feeIncome ? 0 : 1
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LedgerAccountEntity requirePlatformAccount(Long tenantId, String accountType, String currency) {
        return requireOwnerAccount(
                tenantId,
                SubjectTypeEnum.PLATFORM.code(),
                accountType,
                currency,
                buildPlatformAccountNo(tenantId, accountType, currency),
                LedgerDirectionEnum.CREDIT.code(),
                0
        );
    }

    private LedgerAccountEntity requireOwnerAccount(Long tenantId, String ownerType, String accountType,
                                                    String currency, String accountNo, String normalSide, int allowNegative) {
        long ownerId = 0L;
        String normalizedCurrency = normalizeCurrency(currency);
        LedgerAccountEntity account = findOwnerAccount(tenantId, ownerType, ownerId, accountType, normalizedCurrency);
        if (account != null) {
            ensureBalance(account);
            return account;
        }
        account = new LedgerAccountEntity();
        account.setTenantId(tenantId);
        account.setOwnerType(ownerType);
        account.setOwnerId(ownerId);
        account.setAccountType(accountType);
        account.setCurrency(normalizedCurrency);
        account.setAccountNo(accountNo);
        account.setNormalSide(normalSide);
        account.setAllowNegative(allowNegative);
        account.setStatus(StatusEnum.NORMAL.code());
        try {
            insertAccountWithSavepoint(account);
        } catch (DuplicateKeyException ex) {
            account = findOwnerAccount(tenantId, ownerType, ownerId, accountType, normalizedCurrency);
            if (account == null) {
                throw ex;
            }
        }
        ensureBalance(account);
        return account;
    }

    private LedgerAccountEntity findMerchantAccount(Long tenantId, Long merchantId, String accountType, String currency) {
        return findOwnerAccount(tenantId, SubjectTypeEnum.MERCHANT.code(), merchantId, accountType, currency);
    }

    private LedgerAccountEntity findOwnerAccount(Long tenantId, String ownerType, Long ownerId, String accountType, String currency) {
        return baseDao.selectOne(new QueryWrapper<LedgerAccountEntity>()
                .eq("tenant_id", tenantId)
                .eq("owner_type", ownerType)
                .eq("owner_id", ownerId)
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
            insertBalanceWithSavepoint(balance);
        } catch (DuplicateKeyException ignored) {
            // concurrent init
        }
    }

    private String buildMerchantAccountNo(String merchantNo, String accountType, String currency) {
        return String.join("-",
                "M",
                normalizeCodeToken(merchantNo),
                accountTypeToken(accountType),
                normalizeCurrency(currency));
    }

    private void insertAccountWithSavepoint(LedgerAccountEntity account) {
        SavepointExecutor.run(() -> baseDao.insert(account));
    }

    private void insertBalanceWithSavepoint(LedgerBalanceEntity balance) {
        SavepointExecutor.run(() -> ledgerBalanceDao.insert(balance));
    }

    private String buildPspAccountNo(String pspAccountNo, String accountType, String currency) {
        return String.join("-",
                "PSP",
                normalizeCodeToken(pspAccountNo),
                accountTypeToken(accountType),
                normalizeCurrency(currency));
    }

    private String buildInternalAccountNo(Long tenantId, String accountType, String currency) {
        return String.join("-",
                "INT",
                tenantCodeToken(tenantId),
                accountTypeToken(accountType),
                normalizeCurrency(currency));
    }

    private String buildPlatformAccountNo(Long tenantId, String accountType, String currency) {
        return String.join("-",
                "PLT",
                tenantCodeToken(tenantId),
                accountTypeToken(accountType),
                normalizeCurrency(currency));
    }

    private String tenantCodeToken(Long tenantId) {
        if (tenantId == null) {
            throw new IllegalArgumentException("tenantId is required for ledger account no");
        }
        TenantEntity tenant = tenantDao.selectById(tenantId);
        if (tenant == null || StringUtils.isBlank(tenant.getCode())) {
            throw new IllegalStateException("Tenant code is required for ledger account no: " + tenantId);
        }
        return normalizeCodeToken(tenant.getCode());
    }

    private String requireMerchantNo(Long merchantId) {
        if (merchantId == null) {
            throw new IllegalArgumentException("merchantId is required for ledger account no");
        }
        MerchantEntity merchant = merchantDao.selectById(merchantId);
        if (merchant == null || StringUtils.isBlank(merchant.getMerchantNo())) {
            throw new IllegalStateException("Merchant no is required for ledger account no: " + merchantId);
        }
        return merchant.getMerchantNo();
    }

    private String requirePspAccountNo(Long pspAccountId) {
        if (pspAccountId == null) {
            throw new IllegalArgumentException("pspAccountId is required for ledger account no");
        }
        PspAccountEntity pspAccount = pspAccountDao.selectById(pspAccountId);
        if (pspAccount == null || StringUtils.isBlank(pspAccount.getPspAccountNo())) {
            throw new IllegalStateException("PSP account no is required for ledger account no: " + pspAccountId);
        }
        return pspAccount.getPspAccountNo();
    }

    private static String normalizeCodeToken(String value) {
        String normalized = StringUtils.defaultString(value)
                .trim()
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "");
        return StringUtils.defaultIfBlank(normalized, "UNKNOWN");
    }

    private static String accountTypeToken(String accountType) {
        if (LedgerAccountTypeEnum.CLEARING.matches(accountType)) {
            return "CLR";
        }
        if (LedgerAccountTypeEnum.FEE_INCOME.matches(accountType)) {
            return "FEE";
        }
        if (LedgerAccountTypeEnum.FROZEN.matches(accountType)) {
            return "FRZ";
        }
        if (LedgerAccountTypeEnum.PENDING_SETTLE.matches(accountType)) {
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
