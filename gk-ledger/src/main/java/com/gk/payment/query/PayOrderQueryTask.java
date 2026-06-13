package com.gk.payment.query;

import com.gk.quartz.task.ITask;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component("payOrderQueryTask")
@RequiredArgsConstructor
public class PayOrderQueryTask implements ITask {
    private final PspOrderQueryExecutor executor;

    @Override
    public String run(String params) {
        int handled = executor.drainPayOrders();
        return "pay-order-query handled=" + handled;
    }
}
