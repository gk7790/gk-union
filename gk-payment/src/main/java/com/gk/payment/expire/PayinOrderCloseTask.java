package com.gk.payment.expire;

import com.gk.payment.service.PayinOrderService;
import com.gk.common.task.ITask;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 代收超时关单定时任务 */
@Component("payinOrderCloseTask")
@RequiredArgsConstructor
public class PayinOrderCloseTask implements ITask {
    private final PayinOrderService payinOrderService;

    @Override
    public String run(String params) {
        int closed = payinOrderService.drainExpiredPayinOrders();
        return "pay-order-close closed=" + closed;
    }
}
