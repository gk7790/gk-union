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
        var record = execution().record("Close expired payin orders");
        try {
            int closed = payinOrderService.drainExpiredPayinOrders();
            record.step("DRAIN", "Closed orders=" + closed);
            record.complete("Expired order scan completed");
            return "payin-order-close closed=" + closed;
        } catch (RuntimeException exception) {
            record.error("DRAIN", "Expired order scan failed", exception);
            throw exception;
        }
    }
}
