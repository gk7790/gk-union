package com.gk.openapi.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gk.common.enums.SubjectTypeEnum;
import com.gk.ledger.dao.LedgerAccountDao;
import com.gk.ledger.dao.LedgerBalanceDao;
import com.gk.ledger.entity.LedgerAccountEntity;
import com.gk.ledger.entity.LedgerBalanceEntity;
import com.gk.openapi.dto.BalanceResponse;
import com.gk.openapi.security.ApiReqContextHolder;
import com.gk.openapi.service.OpenBalanceService;
import com.gk.openapi.util.ApiAmountUtils;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OpenBalanceServiceImpl implements OpenBalanceService {
    private static final String ACCOUNT_TYPE_MERCHANT_AVAILABLE = "MERCHANT_AVAILABLE";

    private final LedgerAccountDao ledgerAccountDao;
    private final LedgerBalanceDao ledgerBalanceDao;

    @Override
    public List<BalanceResponse> list(String currency) {
        QueryWrapper<LedgerAccountEntity> accountWrapper = new QueryWrapper<LedgerAccountEntity>()
                .eq("tenant_id", ApiReqContextHolder.getTenantId())
                .eq("owner_type", SubjectTypeEnum.MERCHANT.code())
                .eq("owner_id", ApiReqContextHolder.getMerchantId())
                .eq("account_type", ACCOUNT_TYPE_MERCHANT_AVAILABLE)
                .eq("status", 1);
        if (StringUtils.isNotBlank(currency)) {
            accountWrapper.eq("currency", currency.trim().toUpperCase(Locale.ROOT));
        }
        accountWrapper.orderByAsc("currency");
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

        return accounts.stream().map(account -> {
            LedgerBalanceEntity balance = balanceMap.get(account.getId());
            BalanceResponse response = new BalanceResponse();
            response.setAccountNo(account.getAccountNo());
            response.setCurrency(account.getCurrency());
            response.setBalance(ApiAmountUtils.formatCurrencyAmount(balance == null ? null : balance.getBalance(), account.getCurrency()));
            return response;
        }).toList();
    }
}
