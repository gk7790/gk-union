package com.gk.payment.settle;

import com.gk.payment.service.PayinOrderService;
import com.gk.common.task.ITask;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 代收待结算释放定时任务
 * <p>
 * gk-scheduler 中配beanName = {@code paySettleReleaseTask}，建cron 每分钟一次
 */
@Component("paySettleReleaseTask")
@RequiredArgsConstructor
public class PaySettleReleaseTask implements ITask {
    private final PayinOrderService payinOrderService;

    @Override
    public String run(String params) {
        int released = payinOrderService.drainDueSettlements();
        return "pay-settle-release released=" + released;
    }
}
