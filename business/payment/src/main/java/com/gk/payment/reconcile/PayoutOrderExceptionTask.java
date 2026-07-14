package com.gk.payment.reconcile;

import com.gk.payment.service.PayoutOrderService;
import com.gk.common.task.ITask;
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
        var record = execution().record("Mark long-processing payout orders for manual review");
        try {
            int marked = payoutOrderService.drainLongProcessingOrders();
            record.step("DRAIN", "Orders marked for review=" + marked);
            record.complete("Payout exception scan completed");
            return "payout-order-exception manualReview=" + marked;
        } catch (RuntimeException exception) {
            record.error("DRAIN", "Payout exception scan failed", exception);
            throw exception;
        }
    }
}
