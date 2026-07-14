package com.gk.payment.outbox;

import com.gk.common.task.ITask;
import com.gk.infra.mq.entity.MqOutboxEntity;
import com.gk.infra.mq.service.MqOutboxService;
import com.gk.payment.domain.error.PaymentErrorCode;
import com.gk.payment.domain.error.PaymentException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component("payinSubmitOutboxTask")
@RequiredArgsConstructor
public class PayinSubmitOutboxTask implements ITask {
    private static final int DEFAULT_BATCH_SIZE = 20;

    private final MqOutboxService mqOutboxService;
    private final PayinSubmitOutboxConsumer consumer;

    @Override
    public String run(String params) {
        int batchSize = parseBatchSize(params);
        List<MqOutboxEntity> events = mqOutboxService.lockDueEvents(
                PayinSubmitOutboxProducer.EVENT_TYPE,
                batchSize,
                "payin-submit-outbox"
        );
        int success = 0;
        int failed = 0;
        for (MqOutboxEntity event : events) {
            var record = execution().record("Payin outbox event=" + event.getEventId() + ", bizNo=" + event.getBizNo());
            try {
                record.step("CONSUME", "Start consuming outbox payload");
                consumer.consume(event.getPayloadJson());
                record.step("CONSUME", "Payload consumed");
                mqOutboxService.markDone(event.getId());
                success++;
                record.step("MARK_DONE", "Outbox marked done");
                record.complete("Payin submission completed");
            } catch (Exception ex) {
                failed++;
                log.warn("Payin submit outbox consume failed, eventId={}, bizNo={}, err={}",
                        event.getEventId(), event.getBizNo(), ex.getMessage());
                if (isNonRetryable(ex)) {
                    mqOutboxService.markDead(event.getId(), errorCode(ex), ex.getMessage());
                } else {
                    mqOutboxService.markRetry(event.getId(), errorCode(ex), ex.getMessage());
                }
                record.error("CONSUME", "Payin submission failed", ex);
                record.complete("Outbox marked for retry or dead letter");
            }
        }
        return "payin-submit-outbox handled=" + events.size() + ", success=" + success + ", failed=" + failed;
    }

    private boolean isNonRetryable(Exception ex) {
        if (!(ex instanceof PaymentException apiException)) {
            return false;
        }
        PaymentErrorCode code = apiException.getErrorCode();
        return code == PaymentErrorCode.INVALID_REQUEST
                || code == PaymentErrorCode.UNSUPPORTED_METHOD;
    }

    private String errorCode(Exception ex) {
        if (ex instanceof PaymentException apiException) {
            return apiException.getErrorCode().name();
        }
        return ex.getClass().getSimpleName();
    }

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
