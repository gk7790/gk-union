package com.gk.payment.notify;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 商户通知定时扫描触发器。
 * <p>
 * 通过 {@code merchant.notify.enabled=false} 可关闭(默认开启)。
 * 扫描频率由 {@code merchant.notify.scan-interval-ms} 控制(默认 5s)。
 * 多实例部署时各节点都会扫描, 但任务抢占基于数据库行级条件更新, 不会重复发送。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "merchant.notify", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MerchantNotifyScheduler {
    private final MerchantNotifyExecutor executor;

    @Scheduled(
            fixedDelayString = "${merchant.notify.scan-interval-ms:5000}",
            initialDelayString = "${merchant.notify.initial-delay-ms:10000}")
    public void scan() {
        try {
            int handled = executor.dispatchBatch();
            if (handled > 0 && log.isDebugEnabled()) {
                log.debug("Merchant notify dispatched {} task(s)", handled);
            }
        } catch (Exception e) {
            log.error("Merchant notify scan error", e);
        }
    }
}
