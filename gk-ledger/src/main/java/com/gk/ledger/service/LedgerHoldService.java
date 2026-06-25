package com.gk.ledger.service;

import com.gk.common.core.service.CrudService;
import com.gk.ledger.dto.LedgerHoldDTO;
import com.gk.ledger.entity.LedgerHoldEntity;

public interface LedgerHoldService extends CrudService<LedgerHoldEntity, LedgerHoldDTO> {
    /** 扫描过期冻结并标记为待处理，供定时任务调用�?*/
    int drainExpiredHolds();
}
