package com.gk.payment.reconcile;

import com.gk.common.task.ITask;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component("payinOrderQueryTask")
@RequiredArgsConstructor
public class PayinOrderQueryTask implements ITask {
    private final PspOrderQueryExecutor executor;

    @Override
    public String run(String params) {
        var record = execution().record("Query payin orders");
        try {
            int handled = executor.drainPayinOrders();
            record.step("DRAIN", "Queried orders=" + handled);
            record.complete("Payin order query completed");
            return "payin-order-query handled=" + handled;
        } catch (RuntimeException exception) {
            record.error("DRAIN", "Payin order query failed", exception);
            throw exception;
        }
    }
}
