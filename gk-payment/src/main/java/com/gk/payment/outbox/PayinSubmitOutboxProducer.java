package com.gk.payment.outbox;

import com.alibaba.fastjson2.JSON;
import com.gk.common.context.ReqContextHolder;
import com.gk.common.enums.BizTypeEnum;
import com.gk.common.utils.BizKeyUtils;
import com.gk.infra.mq.entity.MqOutboxEntity;
import com.gk.infra.mq.enums.MqOutboxConsumeStatusEnum;
import com.gk.infra.mq.enums.MqOutboxPublishStatusEnum;
import com.gk.infra.mq.service.MqOutboxService;
import com.gk.payment.entity.PayinOrderEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Produces payin submit outbox events.
 */
@Component
@RequiredArgsConstructor
public class PayinSubmitOutboxProducer {
    public static final String EVENT_TYPE = "PAYIN_SUBMIT_REQUESTED";
    public static final String TOPIC = "PAYMENT";

    private final MqOutboxService mqOutboxService;

    public MqOutboxEntity create(PayinOrderEntity order) {
        PayinSubmitOutboxPayload payload = new PayinSubmitOutboxPayload(
                order.getTenantId(),
                order.getMerchantId(),
                order.getMerchantAppId(),
                order.getPayinOrderNo(),
                order.getMerchantOrderNo(),
                order.getCurrency(),
                order.getAmount()
        );
        MqOutboxEntity event = new MqOutboxEntity();
        event.setTenantId(order.getTenantId());
        event.setEventId(BizKeyUtils.genEventId());
        event.setEventType(EVENT_TYPE);
        event.setEventVersion(1);
        event.setSourceService("gk-payment");
        event.setAggregateType(BizTypeEnum.PAYIN_ORDER.code());
        event.setAggregateId(order.getId());
        event.setAggregateNo(order.getPayinOrderNo());
        event.setBizType(BizTypeEnum.PAYIN_ORDER.code());
        event.setBizId(order.getId());
        event.setBizNo(order.getPayinOrderNo());
        event.setTopic(TOPIC);
        event.setTag(EVENT_TYPE);
        event.setMessageKey(order.getTenantId() + ":" + order.getPayinOrderNo() + ":" + EVENT_TYPE);
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

    private String traceId() {
        return ReqContextHolder.getTraceId();
    }
}
