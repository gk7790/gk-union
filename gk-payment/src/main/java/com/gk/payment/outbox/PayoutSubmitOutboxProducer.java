package com.gk.payment.outbox;

import com.alibaba.fastjson2.JSON;
import com.gk.common.context.ReqContextHolder;
import com.gk.payment.domain.enums.BizTypeEnum;
import com.gk.payment.domain.key.BizKeyUtils;
import com.gk.infra.mq.entity.MqOutboxEntity;
import com.gk.infra.mq.enums.MqOutboxConsumeStatusEnum;
import com.gk.infra.mq.enums.MqOutboxPublishStatusEnum;
import com.gk.infra.mq.service.MqOutboxService;
import com.gk.payment.entity.PayoutOrderEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Produces payout submit outbox events.
 * New payout orders are persisted first, then a PAYOUT_SUBMIT_REQUESTED event is
 * created in the same transaction. The task later locks and dispatches the event
 * to the consumer for balance freezing and PSP submission.
 */
@Component
@RequiredArgsConstructor
public class PayoutSubmitOutboxProducer {
    public static final String EVENT_TYPE = "PAYOUT_SUBMIT_REQUESTED";
    public static final String TOPIC = "PAYMENT";

    private final MqOutboxService mqOutboxService;

    /**
     * Creates an idempotent PSP submit event for a new payout order.
     */
    public MqOutboxEntity create(PayoutOrderEntity order) {
        PayoutSubmitOutboxPayload payload = new PayoutSubmitOutboxPayload(
                order.getTenantId(),
                order.getMerchantId(),
                order.getMerchantAppId(),
                order.getPayoutOrderNo(),
                order.getMerchantOrderNo(),
                order.getCurrency(),
                order.getAmount(),
                order.getTotalDebitAmount()
        );
        MqOutboxEntity event = new MqOutboxEntity();
        event.setTenantId(order.getTenantId());
        event.setEventId(BizKeyUtils.genEventId());
        event.setEventType(EVENT_TYPE);
        event.setEventVersion(1);
        event.setSourceService("gk-payment");
        event.setAggregateType(BizTypeEnum.PAYOUT_ORDER.code());
        event.setAggregateId(order.getId());
        event.setAggregateNo(order.getPayoutOrderNo());
        event.setBizType(BizTypeEnum.PAYOUT_ORDER.code());
        event.setBizId(order.getId());
        event.setBizNo(order.getPayoutOrderNo());
        event.setTopic(TOPIC);
        event.setTag(EVENT_TYPE);
        event.setMessageKey(order.getTenantId() + ":" + order.getPayoutOrderNo() + ":" + EVENT_TYPE);
        event.setShardingKey(order.getMerchantId() + ":" + order.getCurrency());
        event.setPayloadJson(JSON.toJSONString(payload));
        event.setPublishStatus(MqOutboxPublishStatusEnum.INIT.code());
        event.setConsumeStatus(MqOutboxConsumeStatusEnum.INIT.code());
        event.setRetryCount(0);
        event.setMaxRetryCount(16);
        event.setNextRetryAt(Instant.now());
        event.setTraceId(traceId());
        return mqOutboxService.createIfAbsent(event);
    }

    /**
     * Carries the current request trace id into the async outbox event.
     */
    private String traceId() {
        return ReqContextHolder.getTraceId();
    }
}
