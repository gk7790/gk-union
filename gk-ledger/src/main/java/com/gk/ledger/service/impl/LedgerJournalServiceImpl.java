package com.gk.ledger.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gk.common.core.service.impl.CrudServiceImpl;
import com.gk.common.model.DynMap;
import com.gk.common.model.PageData;
import com.gk.ledger.dao.LedgerJournalDao;
import com.gk.ledger.dto.LedgerJournalDTO;
import com.gk.ledger.entity.LedgerJournalEntity;
import com.gk.ledger.service.LedgerJournalService;
import com.gk.ledger.support.SubjectDisplayEnricher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LedgerJournalServiceImpl extends CrudServiceImpl<LedgerJournalDao, LedgerJournalEntity, LedgerJournalDTO> implements LedgerJournalService {
    @Autowired(required = false)
    private SubjectDisplayEnricher subjectDisplayEnricher;

    @Override
    public PageData<LedgerJournalDTO> page(DynMap params) {
        PageData<LedgerJournalDTO> page = super.page(params);
        enrichJournals(page.getItems());
        return page;
    }

    @Override
    public List<LedgerJournalDTO> list(DynMap params) {
        List<LedgerJournalDTO> items = super.list(params);
        enrichJournals(items);
        return items;
    }

    @Override
    public LedgerJournalDTO get(Long id) {
        LedgerJournalDTO dto = super.get(id);
        if (dto != null) {
            enrichJournals(List.of(dto));
        }
        return dto;
    }

    private void enrichJournals(List<LedgerJournalDTO> items) {
        if (subjectDisplayEnricher != null) {
            subjectDisplayEnricher.enrichJournals(items);
        }
    }

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
