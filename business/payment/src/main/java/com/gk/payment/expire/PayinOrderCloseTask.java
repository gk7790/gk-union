package com.gk.payment.expire;

import com.gk.common.task.ITask;
import com.gk.payment.service.PayinOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 代收超时关单定时任务
 */
@Component("payinOrderCloseTask")
@RequiredArgsConstructor
public class PayinOrderCloseTask implements ITask {
    private final PayinOrderService payinOrderService;

    @Override
    public String run(String params) {
        int closed = payinOrderService.drainExpiredPayinOrders();
        return "payin-order-close closed=" + closed;
    }
}
