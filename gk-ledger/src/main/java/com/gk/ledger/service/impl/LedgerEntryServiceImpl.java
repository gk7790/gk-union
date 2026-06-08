package com.gk.ledger.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gk.common.core.service.impl.CrudServiceImpl;
import com.gk.common.model.DynMap;
import com.gk.ledger.dao.LedgerEntryDao;
import com.gk.ledger.dto.LedgerEntryDTO;
import com.gk.ledger.entity.LedgerEntryEntity;
import com.gk.ledger.service.LedgerEntryService;
import org.springframework.stereotype.Service;

@Service
public class LedgerEntryServiceImpl extends CrudServiceImpl<LedgerEntryDao, LedgerEntryEntity, LedgerEntryDTO> implements LedgerEntryService {

    @Override
    public QueryWrapper<LedgerEntryEntity> getWrapper(DynMap params) {
        QueryWrapper<LedgerEntryEntity> wrapper = new QueryWrapper<>();
        Long tenantId = params.getLong("tenantId", null);
        Long journalId = params.getLong("journalId", null);
        Long accountId = params.getLong("accountId", null);
        Long ownerId = params.getLong("ownerId", null);
        Long bizId = params.getLong("bizId", null);
        String journalNo = params.getStr("journalNo");
        String accountNo = params.getStr("accountNo");
        String ownerType = params.getStr("ownerType");
        String accountType = params.getStr("accountType");
        String currency = params.getStr("currency");
        String direction = params.getStr("direction");
        String bizType = params.getStr("bizType");
        String bizNo = params.getStr("bizNo");
        String eventType = params.getStr("eventType");

        wrapper.eq(tenantId != null, "tenant_id", tenantId);
        wrapper.eq(journalId != null, "journal_id", journalId);
        wrapper.eq(accountId != null, "account_id", accountId);
        wrapper.eq(ownerId != null, "owner_id", ownerId);
        wrapper.eq(bizId != null, "biz_id", bizId);
        wrapper.eq(StrUtil.isNotBlank(journalNo), "journal_no", journalNo);
        wrapper.eq(StrUtil.isNotBlank(accountNo), "account_no", accountNo);
        wrapper.eq(StrUtil.isNotBlank(ownerType), "owner_type", ownerType);
        wrapper.eq(StrUtil.isNotBlank(accountType), "account_type", accountType);
        wrapper.eq(StrUtil.isNotBlank(currency), "currency", currency);
        wrapper.eq(StrUtil.isNotBlank(direction), "direction", direction);
        wrapper.eq(StrUtil.isNotBlank(bizType), "biz_type", bizType);
        wrapper.eq(StrUtil.isNotBlank(bizNo), "biz_no", bizNo);
        wrapper.eq(StrUtil.isNotBlank(eventType), "event_type", eventType);
        return wrapper;
    }
}
