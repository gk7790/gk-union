package com.gk.psp.balance;

import com.gk.common.task.ITask;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component("pspBalanceTask")
@RequiredArgsConstructor
public class PspBalanceTask implements ITask {
    private final PspBalanceService balanceService;

    @Override
    public String run(String params) {
        var record = execution().record("Refresh PSP balances");
        try {
            int refreshed = balanceService.refreshAll();
            record.step("REFRESH", "Refreshed balances=" + refreshed);
            record.complete("PSP balance refresh completed");
            return "psp-balance refreshed=" + refreshed;
        } catch (RuntimeException exception) {
            record.error("REFRESH", "PSP balance refresh failed", exception);
            throw exception;
        }
    }
}
