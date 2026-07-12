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
        int refreshed = balanceService.refreshAll();
        return "psp-balance refreshed=" + refreshed;
    }
}
