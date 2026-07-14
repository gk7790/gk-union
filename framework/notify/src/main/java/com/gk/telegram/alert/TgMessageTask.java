package com.gk.telegram.alert;

import com.gk.common.task.ITask;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Telegram 消息发送定时任务。
 * <p>在后台定时任务中配置 beanName = {@code tgMessageTask}，建议 10 秒一次。</p>
 */
@Component("tgMessageTask")
@RequiredArgsConstructor
public class TgMessageTask implements ITask {
    private final TgMessageTaskExecutor executor;

    @Override
    public String run(String params) {
        var record = execution().record("Dispatch Telegram messages");
        try {
            int handled = executor.drain(params);
            record.step("DRAIN", "Messages handled=" + handled + ", params=" + params);
            record.complete("Telegram message dispatch completed");
            return "tg-message params=" + params + ", handled=" + handled;
        } catch (RuntimeException exception) {
            record.error("DRAIN", "Telegram message dispatch failed", exception);
            throw exception;
        }
    }
}
