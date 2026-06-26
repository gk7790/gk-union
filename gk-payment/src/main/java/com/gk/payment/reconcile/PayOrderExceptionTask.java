package com.gk.payment.reconcile;

import com.gk.payment.service.PayOrderService;
import com.gk.quartz.task.ITask;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 代收长时间处理中转人工处理定时任务
 */
@Component("payOrderExceptionTask")
@RequiredArgsConstructor
public class PayOrderExceptionTask implements ITask {
    private final PayOrderService payOrderService;

    @Override
    public String run(String params) {
        int marked = payOrderService.drainLongProcessingOrders();
        return "pay-order-exception manualReview=" + marked;
    }
}
