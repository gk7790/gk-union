package com.gk.infra.mq.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.gk.common.core.entity.SimpleEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.Instant;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("mq_outbox")
public class MqOutboxEntity extends SimpleEntity {
    private Long tenantId;
    private String eventId;
    private String eventType;
    private Integer eventVersion;
    private String sourceService;
    private String aggregateType;
    private Long aggregateId;
    private String aggregateNo;
    private String bizType;
    private Long bizId;
    private String bizNo;
    private String topic;
    private String tag;
    private String messageKey;
    private String shardingKey;
    private String producerGroup;
    private Integer delayLevel;
    private Instant deliverAt;
    private String headersJson;
    private String payloadJson;
    private String publishStatus;
    private String consumeStatus;
    private Integer retryCount;
    private Integer maxRetryCount;
    private Instant nextRetryAt;
    private String lockedBy;
    private Instant lockedAt;
    private Instant lockUntil;
    private String rocketmqMsgId;
    private Instant sentAt;
    private Instant consumedAt;
    private Instant deadAt;
    private String lastErrorCode;
    private String lastErrorMsg;
    private String traceId;
    private String remark;
}
