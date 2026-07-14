package com.gk.ledger.hold;

import com.gk.ledger.service.LedgerHoldService;
import com.gk.common.task.ITask;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 账务冻结过期扫描定时任务 */
@Component("ledgerHoldExpireTask")
@RequiredArgsConstructor
public class LedgerHoldExpireTask implements ITask {
    private final LedgerHoldService ledgerHoldService;

    @Override
    public String run(String params) {
        var record = execution().record("Expire ledger holds");
        try {
            int expired = ledgerHoldService.drainExpiredHolds();
            record.step("DRAIN", "Expired holds=" + expired);
            record.complete("Ledger hold expiry scan completed");
            return "ledger-hold-expire expired=" + expired;
        } catch (RuntimeException exception) {
            record.error("DRAIN", "Ledger hold expiry scan failed", exception);
            throw exception;
        }
    }
}
