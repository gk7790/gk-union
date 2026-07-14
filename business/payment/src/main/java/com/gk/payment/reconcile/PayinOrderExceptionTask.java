package com.gk.payment.reconcile;

import com.gk.common.task.ITask;
import com.gk.payment.service.PayinOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 代收长时间处理中转人工处理定时任务
 */
@Component("payinOrderExceptionTask")
@RequiredArgsConstructor
public class PayinOrderExceptionTask implements ITask {
    private final PayinOrderService payinOrderService;

    @Override
    public String run(String params) {
        var record = execution().record("Mark long-processing payin orders for manual review");
        try {
            int marked = payinOrderService.drainLongProcessingOrders();
            record.step("DRAIN", "Orders marked for review=" + marked);
            record.complete("Payin exception scan completed");
            return "payin-order-exception manualReview=" + marked;
        } catch (RuntimeException exception) {
            record.error("DRAIN", "Payin exception scan failed", exception);
            throw exception;
        }
    }
}
