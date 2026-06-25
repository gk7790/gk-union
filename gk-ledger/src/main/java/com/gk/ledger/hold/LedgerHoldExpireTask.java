package com.gk.ledger.hold;

import com.gk.ledger.service.LedgerHoldService;
import com.gk.quartz.task.ITask;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 账务冻结过期扫描定时任务�? */
@Component("ledgerHoldExpireTask")
@RequiredArgsConstructor
public class LedgerHoldExpireTask implements ITask {
    private final LedgerHoldService ledgerHoldService;

    @Override
    public String run(String params) {
        int expired = ledgerHoldService.drainExpiredHolds();
        return "ledger-hold-expire expired=" + expired;
    }
}
