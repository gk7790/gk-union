package com.gk.payment.reconcile;

import com.gk.payment.service.PayoutOrderService;
import com.gk.quartz.task.ITask;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 代付长时间处理中转人工处理定时任务 */
@Component("payoutOrderExceptionTask")
@RequiredArgsConstructor
public class PayoutOrderExceptionTask implements ITask {
    private final PayoutOrderService payoutOrderService;

    @Override
    public String run(String params) {
        int marked = payoutOrderService.drainLongProcessingOrders();
        return "payout-order-exception manualReview=" + marked;
    }
}
