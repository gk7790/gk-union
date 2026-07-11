package com.gk.ledger.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gk.common.core.service.impl.CrudServiceImpl;
import com.gk.common.model.DynMap;
import com.gk.common.model.PageData;
import com.gk.ledger.dao.LedgerBalanceDao;
import com.gk.ledger.dto.LedgerBalanceDTO;
import com.gk.ledger.entity.LedgerBalanceEntity;
import com.gk.ledger.service.LedgerBalanceService;
import com.gk.ledger.support.SubjectDisplayEnricher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LedgerBalanceServiceImpl extends CrudServiceImpl<LedgerBalanceDao, LedgerBalanceEntity, LedgerBalanceDTO> implements LedgerBalanceService {
    @Autowired(required = false)
    private SubjectDisplayEnricher subjectDisplayEnricher;

    @Override
    public PageData<LedgerBalanceDTO> page(DynMap params) {
        PageData<LedgerBalanceDTO> page = super.page(params);
        enrichBalances(page.getItems());
        return page;
    }

    @Override
    public List<LedgerBalanceDTO> list(DynMap params) {
        List<LedgerBalanceDTO> items = super.list(params);
        enrichBalances(items);
        return items;
    }

    @Override
    public LedgerBalanceDTO get(Long id) {
        LedgerBalanceDTO dto = super.get(id);
        if (dto != null) {
            enrichBalances(List.of(dto));
        }
        return dto;
    }

    private void enrichBalances(List<LedgerBalanceDTO> items) {
        if (subjectDisplayEnricher != null) {
            subjectDisplayEnricher.enrichBalances(items);
        }
    }

    @Override
    public QueryWrapper<LedgerBalanceEntity> getWrapper(DynMap params) {
        QueryWrapper<LedgerBalanceEntity> wrapper = new QueryWrapper<>();
        Long tenantId = params.getLong("tenantId", null);
        Long accountId = params.getLong("accountId", null);
        String accountNo = params.getStr("accountNo");
        String currency = params.getStr("currency");

        wrapper.eq(tenantId != null, "tenant_id", tenantId);
        wrapper.eq(accountId != null, "account_id", accountId);
        wrapper.eq(StrUtil.isNotBlank(accountNo), "account_no", accountNo);
        wrapper.eq(StrUtil.isNotBlank(currency), "currency", currency);
        return wrapper;
    }
}
