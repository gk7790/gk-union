package com.gk.payment.query;

import com.gk.payment.service.PayOrderService;
import com.gk.quartz.task.ITask;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 代收待结算释放定时任务。
 * <p>
 * 在 gk-scheduler 中配置 beanName = {@code paySettleReleaseTask}，建议 cron 每分钟一次。
 */
@Component("paySettleReleaseTask")
@RequiredArgsConstructor
public class PaySettleReleaseTask implements ITask {
    private final PayOrderService payOrderService;

    @Override
    public String run(String params) {
        int released = payOrderService.drainDueSettlements();
        return "pay-settle-release released=" + released;
    }
}
