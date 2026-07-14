package com.gk.payment.reconcile;

import com.gk.common.task.ITask;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component("payoutOrderQueryTask")
@RequiredArgsConstructor
public class PayoutOrderQueryTask implements ITask {
    private final PspOrderQueryExecutor executor;

    @Override
    public String run(String params) {
        var record = execution().record("Query payout orders");
        try {
            int handled = executor.drainPayoutOrders();
            record.step("DRAIN", "Queried orders=" + handled);
            record.complete("Payout order query completed");
            return "payout-order-query handled=" + handled;
        } catch (RuntimeException exception) {
            record.error("DRAIN", "Payout order query failed", exception);
            throw exception;
        }
    }
}
