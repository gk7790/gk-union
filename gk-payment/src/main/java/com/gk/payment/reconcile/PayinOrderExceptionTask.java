package com.gk.payment.reconcile;

import com.gk.payment.service.PayinOrderService;
import com.gk.common.task.ITask;
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
        int marked = payinOrderService.drainLongProcessingOrders();
        return "pay-order-exception manualReview=" + marked;
    }
}
