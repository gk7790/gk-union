package com.gk.payment.notify;

import com.gk.common.task.ITask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 商户通知发送定时任Quartz)
 * <p>
 * 通过 gk-scheduler 的定时任务框架调 在后定时任务"中新增一
 * beanName = {@code merchantNotifyTask} 的任 配置 cron 即可(建议 10 秒一
 * 每次触发会排空当前到期的待通知任务, 多实例部署时基于数据库行级抢 不会重复发送
 */
@Slf4j
@Component("merchantNotifyTask")
@RequiredArgsConstructor
public class MerchantNotifyTask implements ITask {
    private final MerchantNotifyExecutor executor;

    @Override
    public String run(String params) {
        int handled = executor.drain();
        return "merchant-notify handled=" + handled;
    }
}
