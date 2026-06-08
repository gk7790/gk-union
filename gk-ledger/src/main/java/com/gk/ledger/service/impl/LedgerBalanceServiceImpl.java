package com.gk.ledger.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gk.common.core.service.impl.CrudServiceImpl;
import com.gk.common.model.DynMap;
import com.gk.ledger.dao.LedgerBalanceDao;
import com.gk.ledger.dto.LedgerBalanceDTO;
import com.gk.ledger.entity.LedgerBalanceEntity;
import com.gk.ledger.service.LedgerBalanceService;
import org.springframework.stereotype.Service;

@Service
public class LedgerBalanceServiceImpl extends CrudServiceImpl<LedgerBalanceDao, LedgerBalanceEntity, LedgerBalanceDTO> implements LedgerBalanceService {

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
