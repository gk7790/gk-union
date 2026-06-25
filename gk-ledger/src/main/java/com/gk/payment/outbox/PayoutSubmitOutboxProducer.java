package com.gk.payment.outbox;

import com.alibaba.fastjson2.JSON;
import com.gk.common.enums.BizTypeEnum;
import com.gk.common.utils.BizKeyUtils;
import com.gk.infra.mq.entity.MqOutboxEntity;
import com.gk.infra.mq.enums.MqOutboxConsumeStatusEnum;
import com.gk.infra.mq.enums.MqOutboxPublishStatusEnum;
import com.gk.infra.mq.service.MqOutboxService;
import com.gk.openapi.security.ApiReqContext;
import com.gk.openapi.security.ApiReqContextHolder;
import com.gk.payment.entity.PayoutOrderEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * 代付提交 outbox 事件生产器�? * <p>
 * OpenAPI 创建正式代付订单时，会先�?{@code payout_order}，再在同一个事务中调用本类创建
 * {@code PAYOUT_SUBMIT_REQUESTED} 事件。事件落库后�?{@link PayoutSubmitOutboxTask}
 * 异步锁定并交�?{@link PayoutSubmitOutboxConsumer} 冻结余额、提�?PSP�? */
@Component
@RequiredArgsConstructor
public class PayoutSubmitOutboxProducer {
    public static final String EVENT_TYPE = "PAYOUT_SUBMIT_REQUESTED";
    public static final String TOPIC = "PAYMENT";

    private final MqOutboxService mqOutboxService;

    /**
     * 为新代付订单创建一条提�?PSP �?outbox 事件�?     * <p>
     * 事件�?{@code tenant_id + biz_type + biz_no + event_type} 做业务幂等，
     * 重复调用�?{@link MqOutboxService#createIfAbsent(MqOutboxEntity)} 会返回已有事件�?     */
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
        event.setSourceService("gk-ledger");
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
     * 从当�?OpenAPI 请求上下文带�?traceId，方便异步任务和原始请求串联排查�?     */
    private String traceId() {
        ApiReqContext context = ApiReqContextHolder.get();
        return context == null ? null : context.getTraceId();
    }
}
