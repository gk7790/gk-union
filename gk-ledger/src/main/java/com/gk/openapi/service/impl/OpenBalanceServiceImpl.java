package com.gk.openapi.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gk.ledger.dao.LedgerAccountDao;
import com.gk.ledger.dao.LedgerBalanceDao;
import com.gk.ledger.entity.LedgerAccountEntity;
import com.gk.ledger.entity.LedgerBalanceEntity;
import com.gk.openapi.dto.BalanceResponse;
import com.gk.openapi.security.OpenApiRequestContextHolder;
import com.gk.openapi.service.OpenBalanceService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OpenBalanceServiceImpl implements OpenBalanceService {
    private final LedgerAccountDao ledgerAccountDao;
    private final LedgerBalanceDao ledgerBalanceDao;

    @Override
    public List<BalanceResponse> list(String currency) {
        QueryWrapper<LedgerAccountEntity> accountWrapper = new QueryWrapper<LedgerAccountEntity>()
                .eq("tenant_id", OpenApiRequestContextHolder.getTenantId())
                .eq("owner_type", "MERCHANT")
                .eq("owner_id", OpenApiRequestContextHolder.getMerchantId())
                .eq("status", 1);
        if (StringUtils.isNotBlank(currency)) {
            accountWrapper.eq("currency", currency);
        }
        List<LedgerAccountEntity> accounts = ledgerAccountDao.selectList(accountWrapper);
        if (accounts.isEmpty()) {
            return Collections.emptyList();
        }

        return accounts.stream().map(account -> {
            LedgerBalanceEntity balance = ledgerBalanceDao.selectOne(
                    new QueryWrapper<LedgerBalanceEntity>()
                            .eq("tenant_id", account.getTenantId())
                            .eq("account_id", account.getId())
                            .last("limit 1")
            );
            BalanceResponse response = new BalanceResponse();
            response.setAccountNo(account.getAccountNo());
            response.setAccountType(account.getAccountType());
            response.setCurrency(account.getCurrency());
            if (balance != null) {
                response.setBalance(balance.getBalance());
                response.setDebitTotal(balance.getDebitTotal());
                response.setCreditTotal(balance.getCreditTotal());
            }
            return response;
        }).toList();
    }
}
