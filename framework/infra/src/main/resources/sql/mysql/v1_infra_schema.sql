-- gk-union MySQL schema generated from the provided MySQL dump.
-- Source: pasted-text.txt. Broken comments and Navicat metadata are intentionally omitted.

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `mq_consume_record`;
CREATE TABLE `mq_consume_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `event_id` varchar(64) NOT NULL,
  `topic` varchar(128) NOT NULL,
  `tag` varchar(128) NOT NULL,
  `message_key` varchar(256) NOT NULL,
  `consumer_group` varchar(128) NOT NULL,
  `consumer_name` varchar(128) NULL DEFAULT NULL,
  `consume_status` varchar(32) NOT NULL DEFAULT 'PROCESSING',
  `retry_count` int NOT NULL DEFAULT 0,
  `first_consumed_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `last_consumed_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `success_at` datetime(3) NULL DEFAULT NULL,
  `last_error_code` varchar(64) NULL DEFAULT NULL,
  `last_error_msg` varchar(1024) NULL DEFAULT NULL,
  `rocketmq_msg_id` varchar(128) NULL DEFAULT NULL,
  `trace_id` varchar(128) NULL DEFAULT NULL,
  `created_by` bigint NULL DEFAULT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_by` bigint NULL DEFAULT NULL,
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_mq_consume_event_group`(`event_id` ASC, `consumer_group` ASC) USING BTREE,
  INDEX `idx_mq_consume_message_group`(`message_key` ASC, `consumer_group` ASC) USING BTREE,
  INDEX `idx_mq_consume_status`(`consume_status` ASC, `last_consumed_at` ASC) USING BTREE,
  INDEX `idx_mq_consume_topic_tag`(`topic` ASC, `tag` ASC, `consumer_group` ASC) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `mq_outbox`;
CREATE TABLE `mq_outbox` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `event_id` varchar(64) NOT NULL,
  `event_type` varchar(64) NOT NULL,
  `event_version` int NOT NULL DEFAULT 1,
  `source_service` varchar(64) NOT NULL,
  `aggregate_type` varchar(64) NOT NULL,
  `aggregate_id` bigint NULL DEFAULT NULL,
  `aggregate_no` varchar(128) NOT NULL,
  `biz_type` varchar(64) NOT NULL,
  `biz_id` bigint NULL DEFAULT NULL,
  `biz_no` varchar(128) NOT NULL,
  `topic` varchar(128) NOT NULL,
  `tag` varchar(128) NOT NULL,
  `message_key` varchar(256) NOT NULL,
  `sharding_key` varchar(128) NULL DEFAULT NULL,
  `producer_group` varchar(128) NULL DEFAULT NULL,
  `delay_level` int NULL DEFAULT NULL,
  `deliver_at` datetime(3) NULL DEFAULT NULL,
  `headers_json` json NULL,
  `payload_json` json NOT NULL,
  `publish_status` varchar(32) NOT NULL DEFAULT 'INIT',
  `consume_status` varchar(32) NOT NULL DEFAULT 'INIT',
  `retry_count` int NOT NULL DEFAULT 0,
  `max_retry_count` int NOT NULL DEFAULT 16,
  `next_retry_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `locked_by` varchar(128) NULL DEFAULT NULL,
  `locked_at` datetime(3) NULL DEFAULT NULL,
  `lock_until` datetime(3) NULL DEFAULT NULL,
  `rocketmq_msg_id` varchar(128) NULL DEFAULT NULL,
  `sent_at` datetime(3) NULL DEFAULT NULL,
  `consumed_at` datetime(3) NULL DEFAULT NULL,
  `dead_at` datetime(3) NULL DEFAULT NULL,
  `last_error_code` varchar(64) NULL DEFAULT NULL,
  `last_error_msg` varchar(1024) NULL DEFAULT NULL,
  `trace_id` varchar(128) NULL DEFAULT NULL,
  `remark` varchar(512) NULL DEFAULT NULL,
  `created_by` bigint NULL DEFAULT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_by` bigint NULL DEFAULT NULL,
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_mq_outbox_event`(`event_id` ASC) USING BTREE,
  UNIQUE INDEX `uk_mq_outbox_biz_event`(`tenant_id` ASC, `biz_type` ASC, `biz_no` ASC, `event_type` ASC) USING BTREE,
  INDEX `idx_mq_outbox_publish_scan`(`publish_status` ASC, `next_retry_at` ASC, `id` ASC) USING BTREE,
  INDEX `idx_mq_outbox_consume_scan`(`consume_status` ASC, `next_retry_at` ASC, `id` ASC) USING BTREE,
  INDEX `idx_mq_outbox_lock`(`lock_until` ASC, `locked_by` ASC) USING BTREE,
  INDEX `idx_mq_outbox_topic_tag`(`topic` ASC, `tag` ASC, `created_at` ASC) USING BTREE,
  INDEX `idx_mq_outbox_aggregate`(`tenant_id` ASC, `aggregate_type` ASC, `aggregate_no` ASC) USING BTREE,
  INDEX `idx_mq_outbox_message_key`(`message_key` ASC) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `sys_login_ip_whitelist`;
CREATE TABLE `sys_login_ip_whitelist` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `subject_type` varchar(32) NOT NULL,
  `tenant_id` bigint NULL DEFAULT NULL,
  `merchant_id` bigint NULL DEFAULT NULL,
  `subject_id` bigint NULL DEFAULT NULL,
  `rule_name` varchar(128) NULL DEFAULT NULL,
  `ip_pattern` varchar(128) NOT NULL,
  `status` tinyint NOT NULL DEFAULT 1,
  `remark` varchar(512) NULL DEFAULT NULL,
  `created_by` bigint NULL DEFAULT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_by` bigint NULL DEFAULT NULL,
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_login_ip_subject`(`subject_type` ASC, `tenant_id` ASC, `merchant_id` ASC, `subject_id` ASC, `status` ASC) USING BTREE,
  INDEX `idx_login_ip_status`(`status` ASC, `created_at` ASC) USING BTREE,
  CONSTRAINT `chk_login_ip_status` CHECK (`status` in (1,2,3)),
  CONSTRAINT `chk_login_ip_subject_type` CHECK (`subject_type` in ('PLATFORM','TENANT','MERCHANT'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET FOREIGN_KEY_CHECKS = 1;
