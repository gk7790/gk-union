package com.gk.payment.outbox;

import com.gk.infra.mq.entity.MqOutboxEntity;
import com.gk.infra.mq.service.MqOutboxService;
import com.gk.openapi.error.ApiErrorCode;
import com.gk.openapi.error.ApiException;
import com.gk.quartz.task.ITask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 代付提交 outbox 定时消费任务。
 * <p>
 * 该任务由 Quartz 调度触发，负责从 {@code mq_outbox} 中锁定到期的
 * {@link PayoutSubmitOutboxProducer#EVENT_TYPE} 事件，并把每条事件交给
 * {@link PayoutSubmitOutboxConsumer} 执行业务处理。任务本身只负责调度、统计和
 * outbox 消费状态更新，不直接操作代付订单业务。
 */
@Slf4j
@Component("payoutSubmitOutboxTask")
@RequiredArgsConstructor
public class PayoutSubmitOutboxTask implements ITask {
    private static final int DEFAULT_BATCH_SIZE = 20;

    private final MqOutboxService mqOutboxService;
    private final PayoutSubmitOutboxConsumer consumer;

    /**
     * 扫描、锁定并消费一批到期 outbox 事件。
     * <p>
     * 成功消费后标记 {@code DONE}；可重试异常标记 {@code FAILED} 并等待下次调度；
     * 参数错误、余额不足、支付方案不可用等确定性失败标记 {@code DEAD}，避免无意义重试。
     */
    @Override
    public String run(String params) {
        int batchSize = parseBatchSize(params);
        List<MqOutboxEntity> events = mqOutboxService.lockDueEvents(PayoutSubmitOutboxProducer.EVENT_TYPE, batchSize, "payout-submit-outbox");
        int success = 0;
        int failed = 0;
        for (MqOutboxEntity event : events) {
            try {
                consumer.consume(event.getPayloadJson());
                mqOutboxService.markDone(event.getId());
                success++;
            } catch (Exception ex) {
                failed++;
                log.warn("Payout submit outbox consume failed, eventId={}, bizNo={}, err={}",
                        event.getEventId(), event.getBizNo(), ex.getMessage());
                if (isNonRetryable(ex)) {
                    mqOutboxService.markDead(event.getId(), errorCode(ex), ex.getMessage());
                } else {
                    mqOutboxService.markRetry(event.getId(), errorCode(ex), ex.getMessage());
                }
            }
        }
        return "payout-submit-outbox handled=" + events.size() + ", success=" + success + ", failed=" + failed;
    }

    /**
     * 判断异常是否属于重试也无法恢复的业务失败。
     */
    private boolean isNonRetryable(Exception ex) {
        if (!(ex instanceof ApiException apiException)) {
            return false;
        }
        ApiErrorCode code = apiException.getErrorCode();
        return code == ApiErrorCode.INVALID_REQUEST
                || code == ApiErrorCode.INSUFFICIENT_BALANCE
                || code == ApiErrorCode.UNSUPPORTED_METHOD;
    }

    /**
     * 转换异常为 outbox 最近一次失败码，便于后台排查和重试决策。
     */
    private String errorCode(Exception ex) {
        if (ex instanceof ApiException apiException) {
            return apiException.getErrorCode().name();
        }
        return ex.getClass().getSimpleName();
    }

    /**
     * 解析 Quartz 参数中的批量大小，限制在 1 到 100，避免单次任务处理过多事件。
     */
    private int parseBatchSize(String params) {
        if (StringUtils.isBlank(params)) {
            return DEFAULT_BATCH_SIZE;
        }
        try {
            return Math.max(1, Math.min(100, Integer.parseInt(params.trim())));
        } catch (NumberFormatException ignored) {
            return DEFAULT_BATCH_SIZE;
        }
    }
}
