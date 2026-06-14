package com.gk.openapi.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gk.common.enums.SubjectTypeEnum;
import com.gk.infra.enums.StatusEnum;
import com.gk.ledger.dao.LedgerAccountDao;
import com.gk.ledger.dao.LedgerBalanceDao;
import com.gk.ledger.entity.LedgerAccountEntity;
import com.gk.ledger.entity.LedgerBalanceEntity;
import com.gk.ledger.enums.LedgerAccountTypeEnum;
import com.gk.openapi.dto.BalanceResponse;
import com.gk.openapi.security.ApiReqContextHolder;
import com.gk.openapi.service.OpenBalanceService;
import com.gk.openapi.util.ApiAmountUtils;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OpenBalanceServiceImpl implements OpenBalanceService {

    private final LedgerAccountDao ledgerAccountDao;
    private final LedgerBalanceDao ledgerBalanceDao;

    @Override
    public List<BalanceResponse> list(String currency) {
        QueryWrapper<LedgerAccountEntity> accountWrapper = new QueryWrapper<LedgerAccountEntity>()
                .eq("tenant_id", ApiReqContextHolder.getTenantId())
                .eq("owner_type", SubjectTypeEnum.MERCHANT.code())
                .eq("owner_id", ApiReqContextHolder.getMerchantId())
                .in("account_type",
                        LedgerAccountTypeEnum.MERCHANT_AVAILABLE.code(),
                        LedgerAccountTypeEnum.MERCHANT_PENDING_SETTLE.code())
                .eq("status", StatusEnum.NORMAL.code());
        if (StringUtils.isNotBlank(currency)) {
            accountWrapper.eq("currency", currency.trim().toUpperCase(Locale.ROOT));
        }
        accountWrapper.orderByAsc("currency", "account_type");
        List<LedgerAccountEntity> accounts = ledgerAccountDao.selectList(accountWrapper);
        if (accounts.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> accountIds = accounts.stream().map(LedgerAccountEntity::getId).toList();
        Map<Long, LedgerBalanceEntity> balanceMap = ledgerBalanceDao.selectList(
                        new QueryWrapper<LedgerBalanceEntity>()
                                .eq("tenant_id", ApiReqContextHolder.getTenantId())
                                .in("account_id", accountIds)
                ).stream()
                .collect(Collectors.toMap(LedgerBalanceEntity::getAccountId, Function.identity(), (left, right) -> left));

        Map<String, BalanceResponse> byCurrency = new LinkedHashMap<>();
        for (LedgerAccountEntity account : accounts) {
            BalanceResponse response = byCurrency.computeIfAbsent(account.getCurrency(), key -> {
                BalanceResponse item = new BalanceResponse();
                item.setCurrency(key);
                item.setBalance(formatMoney(BigDecimal.ZERO, key));
                item.setPendingSettleBalance(formatMoney(BigDecimal.ZERO, key));
                return item;
            });
            LedgerBalanceEntity balance = balanceMap.get(account.getId());
            BigDecimal amount = balance == null ? BigDecimal.ZERO : balance.getBalance();
            if (LedgerAccountTypeEnum.MERCHANT_AVAILABLE.matches(account.getAccountType())) {
                response.setAccountNo(account.getAccountNo());
                response.setBalance(formatMoney(amount, account.getCurrency()));
            } else if (LedgerAccountTypeEnum.MERCHANT_PENDING_SETTLE.matches(account.getAccountType())) {
                response.setPendingSettleBalance(formatMoney(amount, account.getCurrency()));
            }
        }
        return new ArrayList<>(byCurrency.values());
    }

    private String formatMoney(BigDecimal value, String currency) {
        return ApiAmountUtils.formatCurrencyAmount(value, currency);
    }
}
