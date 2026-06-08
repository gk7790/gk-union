package com.gk.ledger.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gk.common.core.service.impl.CrudServiceImpl;
import com.gk.common.model.DynMap;
import com.gk.ledger.dao.LedgerAccountDao;
import com.gk.ledger.dto.LedgerAccountDTO;
import com.gk.ledger.entity.LedgerAccountEntity;
import com.gk.ledger.service.LedgerAccountService;
import org.springframework.stereotype.Service;

@Service
public class LedgerAccountServiceImpl extends CrudServiceImpl<LedgerAccountDao, LedgerAccountEntity, LedgerAccountDTO> implements LedgerAccountService {

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
}
