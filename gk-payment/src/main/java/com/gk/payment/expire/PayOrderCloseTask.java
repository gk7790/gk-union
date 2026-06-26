package com.gk.payment.expire;

import com.gk.payment.service.PayOrderService;
import com.gk.common.task.ITask;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 代收超时关单定时任务 */
@Component("payOrderCloseTask")
@RequiredArgsConstructor
public class PayOrderCloseTask implements ITask {
    private final PayOrderService payOrderService;

    @Override
    public String run(String params) {
        int closed = payOrderService.drainExpiredPayOrders();
        return "pay-order-close closed=" + closed;
    }
}
