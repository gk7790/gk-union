package com.gk.ledger.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gk.common.core.service.impl.CrudServiceImpl;
import com.gk.common.model.DynMap;
import com.gk.ledger.dao.LedgerJournalDao;
import com.gk.ledger.dto.LedgerJournalDTO;
import com.gk.ledger.entity.LedgerJournalEntity;
import com.gk.ledger.service.LedgerJournalService;
import org.springframework.stereotype.Service;

@Service
public class LedgerJournalServiceImpl extends CrudServiceImpl<LedgerJournalDao, LedgerJournalEntity, LedgerJournalDTO> implements LedgerJournalService {

    @Override
    public QueryWrapper<LedgerJournalEntity> getWrapper(DynMap params) {
        QueryWrapper<LedgerJournalEntity> wrapper = new QueryWrapper<>();
        Long tenantId = params.getLong("tenantId", null);
        Long bizId = params.getLong("bizId", null);
        String journalNo = params.getStr("journalNo");
        String bizType = params.getStr("bizType");
        String bizNo = params.getStr("bizNo");
        String eventType = params.getStr("eventType");
        String currency = params.getStr("currency");
        String status = params.getStr("status");
        String sourceType = params.getStr("sourceType");
        String traceId = params.getStr("traceId");

        wrapper.eq(tenantId != null, "tenant_id", tenantId);
        wrapper.eq(bizId != null, "biz_id", bizId);
        wrapper.eq(StrUtil.isNotBlank(journalNo), "journal_no", journalNo);
        wrapper.eq(StrUtil.isNotBlank(bizType), "biz_type", bizType);
        wrapper.eq(StrUtil.isNotBlank(bizNo), "biz_no", bizNo);
        wrapper.eq(StrUtil.isNotBlank(eventType), "event_type", eventType);
        wrapper.eq(StrUtil.isNotBlank(currency), "currency", currency);
        wrapper.eq(StrUtil.isNotBlank(status), "status", status);
        wrapper.eq(StrUtil.isNotBlank(sourceType), "source_type", sourceType);
        wrapper.eq(StrUtil.isNotBlank(traceId), "trace_id", traceId);
        return wrapper;
    }
}
