package com.gk.ledger.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.gk.common.core.service.impl.CrudServiceImpl;
import com.gk.common.model.DynMap;
import com.gk.common.model.PageData;
import com.gk.common.task.TaskExecutionRecord;
import com.gk.common.task.TaskExecutions;
import com.gk.ledger.dao.LedgerHoldDao;
import com.gk.ledger.dto.LedgerHoldDTO;
import com.gk.ledger.entity.LedgerHoldEntity;
import com.gk.ledger.enums.LedgerHoldStatusEnum;
import com.gk.ledger.service.LedgerHoldService;
import com.gk.ledger.support.SubjectDisplayEnricher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class LedgerHoldServiceImpl extends CrudServiceImpl<LedgerHoldDao, LedgerHoldEntity, LedgerHoldDTO> implements LedgerHoldService {
    private static final int EXPIRED_HOLD_DRAIN_BATCH = 50;
    private static final String ORDER_HOLD_SCOPE = "ORDER";
    private static final String EXPIRED_REASON = "Ledger hold expired and requires manual handling";
    @Autowired(required = false)
    private SubjectDisplayEnricher subjectDisplayEnricher;

    @Override
    public PageData<LedgerHoldDTO> page(DynMap params) {
        PageData<LedgerHoldDTO> page = super.page(params);
        enrichHolds(page.getItems());
        return page;
    }

    @Override
    public List<LedgerHoldDTO> list(DynMap params) {
        List<LedgerHoldDTO> items = super.list(params);
        enrichHolds(items);
        return items;
    }

    @Override
    public LedgerHoldDTO get(Long id) {
        LedgerHoldDTO dto = super.get(id);
        if (dto != null) {
            enrichHolds(List.of(dto));
        }
        return dto;
    }

    private void enrichHolds(List<LedgerHoldDTO> items) {
        if (subjectDisplayEnricher != null) {
            subjectDisplayEnricher.enrichHolds(items);
        }
    }

    @Override
    public QueryWrapper<LedgerHoldEntity> getWrapper(DynMap params) {
        QueryWrapper<LedgerHoldEntity> wrapper = new QueryWrapper<>();
        Long tenantId = params.getLong("tenantId", null);
        Long ownerId = params.getLong("ownerId", null);
        Long bizId = params.getLong("bizId", null);
        String holdNo = params.getStr("holdNo");
        String ownerType = params.getStr("ownerType");
        String currency = params.getStr("currency");
        String bizType = params.getStr("bizType");
        String bizNo = params.getStr("bizNo");
        String holdReason = params.getStr("holdReason");
        String holdScope = params.getStr("holdScope");
        String status = params.getStr("status");

        wrapper.eq(tenantId != null, "tenant_id", tenantId);
        wrapper.eq(ownerId != null, "owner_id", ownerId);
        wrapper.eq(bizId != null, "biz_id", bizId);
        wrapper.eq(StrUtil.isNotBlank(holdNo), "hold_no", holdNo);
        wrapper.eq(StrUtil.isNotBlank(ownerType), "owner_type", ownerType);
        wrapper.eq(StrUtil.isNotBlank(currency), "currency", currency);
        wrapper.eq(StrUtil.isNotBlank(bizType), "biz_type", bizType);
        wrapper.eq(StrUtil.isNotBlank(bizNo), "biz_no", bizNo);
        wrapper.eq(StrUtil.isNotBlank(holdReason), "hold_reason", holdReason);
        wrapper.eq(StrUtil.isNotBlank(holdScope), "hold_scope", holdScope);
        wrapper.eq(StrUtil.isNotBlank(status), "status", status);
        return wrapper;
    }

    @Override
    public int drainExpiredHolds() {
        Instant now = Instant.now();
        List<LedgerHoldEntity> holds = baseDao.selectList(new QueryWrapper<LedgerHoldEntity>()
                .eq("status", LedgerHoldStatusEnum.HOLDING.code())
                .isNotNull("expired_at")
                .le("expired_at", now)
                .orderByAsc("expired_at", "id")
                .last("limit " + EXPIRED_HOLD_DRAIN_BATCH));
        int expired = 0;
        for (LedgerHoldEntity hold : holds) {
            TaskExecutionRecord executionRecord = TaskExecutions.current().record(
                    "Ledger hold=" + hold.getHoldNo() + ", bizNo=" + hold.getBizNo());
            if (ORDER_HOLD_SCOPE.equalsIgnoreCase(StrUtil.blankToDefault(hold.getHoldScope(), ""))) {
                executionRecord.complete("Skipped order hold; order compensation owns its lifecycle");
                continue;
            }
            if (markExpired(hold)) {
                expired++;
                executionRecord.complete("Ledger hold marked expired");
            } else {
                executionRecord.complete("Skipped because hold state changed concurrently");
            }
        }
        return expired;
    }

    private boolean markExpired(LedgerHoldEntity hold) {
        UpdateWrapper<LedgerHoldEntity> wrapper = new UpdateWrapper<>();
        wrapper.eq("id", hold.getId())
                .eq("status", LedgerHoldStatusEnum.HOLDING.code())
                .set("status", LedgerHoldStatusEnum.EXPIRED.code())
                .set("reason", EXPIRED_REASON);
        return baseDao.update(null, wrapper) > 0;
    }
}
