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
        int handled = executor.drainPayinOrders();
        return "payin-order-query handled=" + handled;
    }
}
