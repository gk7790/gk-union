-- Database outbox schema.
-- V1 can be consumed by Quartz inside the monolith.
-- Later versions can publish the same rows to RocketMQ without changing business tables.

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS mq_outbox (
    id bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    tenant_id bigint NOT NULL COMMENT '租户ID',
    event_id varchar(64) NOT NULL COMMENT '事件ID，全局唯一，建议雪花ID或UUID',
    event_type varchar(64) NOT NULL COMMENT '事件类型: PAYIN_SUCCESS/PAYOUT_FAILED/LEDGER_POSTED等',
    event_version int NOT NULL DEFAULT 1 COMMENT '事件版本',
    source_service varchar(64) NOT NULL COMMENT '事件来源模块: gk-ledger/gk-notify/gk-admin等',

    aggregate_type varchar(64) NOT NULL COMMENT '聚合类型: PAYIN_ORDER/PAYOUT_ORDER/LEDGER_JOURNAL/SETTLE_BATCH等',
    aggregate_id bigint NULL DEFAULT NULL COMMENT '聚合ID',
    aggregate_no varchar(128) NOT NULL COMMENT '聚合编号/业务编号',
    biz_type varchar(64) NOT NULL COMMENT '业务类型',
    biz_id bigint NULL DEFAULT NULL COMMENT '业务ID',
    biz_no varchar(128) NOT NULL COMMENT '业务编号',

    topic varchar(128) NOT NULL COMMENT 'RocketMQ Topic，V1也按该字段分类',
    tag varchar(128) NOT NULL COMMENT 'RocketMQ Tag，通常等于event_type',
    message_key varchar(256) NOT NULL COMMENT 'RocketMQ Key，建议tenantId:bizNo:eventType',
    sharding_key varchar(128) NULL DEFAULT NULL COMMENT '顺序消息分片键，如订单号或商户号',
    producer_group varchar(128) NULL DEFAULT NULL COMMENT 'RocketMQ Producer Group',
    delay_level int NULL DEFAULT NULL COMMENT 'RocketMQ延迟级别，NULL表示立即投递',
    deliver_at datetime(3) NULL DEFAULT NULL COMMENT '期望投递时间，V1定时任务可按该字段延迟处理',

    headers_json json NULL COMMENT '消息头JSON',
    payload_json json NOT NULL COMMENT '消息体JSON',

    publish_status varchar(32) NOT NULL DEFAULT 'INIT' COMMENT '发布状态: INIT/LOCKED/SENT/FAILED/DEAD',
    consume_status varchar(32) NOT NULL DEFAULT 'INIT' COMMENT 'V1本地消费状态: INIT/LOCKED/DONE/FAILED/DEAD/SKIPPED',
    retry_count int NOT NULL DEFAULT 0 COMMENT '发布或本地消费重试次数',
    max_retry_count int NOT NULL DEFAULT 16 COMMENT '最大重试次数',
    next_retry_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '下次重试时间',

    locked_by varchar(128) NULL DEFAULT NULL COMMENT '锁定节点',
    locked_at datetime(3) NULL DEFAULT NULL COMMENT '锁定时间',
    lock_until datetime(3) NULL DEFAULT NULL COMMENT '锁定过期时间',

    rocketmq_msg_id varchar(128) NULL DEFAULT NULL COMMENT 'RocketMQ消息ID',
    sent_at datetime(3) NULL DEFAULT NULL COMMENT '发送RocketMQ成功时间',
    consumed_at datetime(3) NULL DEFAULT NULL COMMENT 'V1本地消费完成时间',
    dead_at datetime(3) NULL DEFAULT NULL COMMENT '进入死信时间',
    last_error_code varchar(64) NULL DEFAULT NULL COMMENT '最后错误码',
    last_error_msg varchar(1024) NULL DEFAULT NULL COMMENT '最后错误信息',

    trace_id varchar(128) NULL DEFAULT NULL COMMENT '链路追踪ID',
    remark varchar(512) NULL DEFAULT NULL COMMENT '备注',
    created_by bigint NULL DEFAULT NULL COMMENT '创建人ID',
    created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_by bigint NULL DEFAULT NULL COMMENT '更新人ID',
    updated_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_mq_outbox_event (event_id),
    UNIQUE KEY uk_mq_outbox_biz_event (tenant_id, biz_type, biz_no, event_type),
    KEY idx_mq_outbox_publish_scan (publish_status, next_retry_at, id),
    KEY idx_mq_outbox_consume_scan (consume_status, next_retry_at, id),
    KEY idx_mq_outbox_lock (lock_until, locked_by),
    KEY idx_mq_outbox_topic_tag (topic, tag, created_at),
    KEY idx_mq_outbox_aggregate (tenant_id, aggregate_type, aggregate_no),
    KEY idx_mq_outbox_message_key (message_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息Outbox事件表';

CREATE TABLE IF NOT EXISTS mq_consume_record (
    id bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    tenant_id bigint NOT NULL COMMENT '租户ID',
    event_id varchar(64) NOT NULL COMMENT '事件ID',
    topic varchar(128) NOT NULL COMMENT 'Topic',
    tag varchar(128) NOT NULL COMMENT 'Tag',
    message_key varchar(256) NOT NULL COMMENT '消息Key',
    consumer_group varchar(128) NOT NULL COMMENT '消费者组',
    consumer_name varchar(128) NULL DEFAULT NULL COMMENT '消费者名称/处理器名称',
    consume_status varchar(32) NOT NULL DEFAULT 'PROCESSING' COMMENT '消费状态: PROCESSING/SUCCESS/FAILED/DEAD',
    retry_count int NOT NULL DEFAULT 0 COMMENT '消费重试次数',
    first_consumed_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '首次消费时间',
    last_consumed_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '最后消费时间',
    success_at datetime(3) NULL DEFAULT NULL COMMENT '消费成功时间',
    last_error_code varchar(64) NULL DEFAULT NULL COMMENT '最后错误码',
    last_error_msg varchar(1024) NULL DEFAULT NULL COMMENT '最后错误信息',
    rocketmq_msg_id varchar(128) NULL DEFAULT NULL COMMENT 'RocketMQ消息ID',
    trace_id varchar(128) NULL DEFAULT NULL COMMENT '链路追踪ID',
    created_by bigint NULL DEFAULT NULL COMMENT '创建人ID',
    created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_by bigint NULL DEFAULT NULL COMMENT '更新人ID',
    updated_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_mq_consume_event_group (event_id, consumer_group),
    KEY idx_mq_consume_message_group (message_key, consumer_group),
    KEY idx_mq_consume_status (consume_status, last_consumed_at),
    KEY idx_mq_consume_topic_tag (topic, tag, consumer_group)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息消费幂等记录表';
