package com.gk.payment.reconcile;

import com.gk.quartz.task.ITask;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component("payoutOrderQueryTask")
@RequiredArgsConstructor
public class PayoutOrderQueryTask implements ITask {
    private final PspOrderQueryExecutor executor;

    @Override
    public String run(String params) {
        int handled = executor.drainPayoutOrders();
        return "payout-order-query handled=" + handled;
    }
}
