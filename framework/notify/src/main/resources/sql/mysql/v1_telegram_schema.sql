-- gk-union MySQL schema generated from the provided MySQL dump.
-- Source: pasted-text.txt. Broken comments and Navicat metadata are intentionally omitted.

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `tg_account`;
CREATE TABLE `tg_account` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `bot_id` bigint NOT NULL,
  `tg_user_id` bigint NOT NULL,
  `tg_username` varchar(64) NULL DEFAULT NULL,
  `language_code` varchar(16) NULL DEFAULT NULL,
  `user_id` bigint NOT NULL,
  `subject_id` bigint NOT NULL,
  `status` tinyint NOT NULL DEFAULT 1,
  `bound_at` datetime(3) NULL DEFAULT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_tg_account_bot_user`(`bot_id` ASC, `tg_user_id` ASC) USING BTREE,
  INDEX `idx_tg_account_user`(`user_id` ASC, `status` ASC) USING BTREE,
  INDEX `idx_tg_account_subject`(`subject_id` ASC, `status` ASC) USING BTREE,
  INDEX `idx_tg_account_tenant`(`tenant_id` ASC, `status` ASC) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `tg_bot`;
CREATE TABLE `tg_bot` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `owner_scope` varchar(16) NOT NULL DEFAULT 'PLATFORM',
  `tenant_id` bigint NULL DEFAULT 0,
  `bot_no` varchar(64) NOT NULL,
  `name` varchar(128) NULL DEFAULT NULL,
  `username` varchar(64) NOT NULL,
  `bot_user_id` bigint NULL DEFAULT NULL,
  `token_cipher` varchar(512) NOT NULL,
  `token_hash` varchar(128) NOT NULL,
  `secret_token` varchar(128) NULL DEFAULT NULL,
  `webhook_url` varchar(512) NULL DEFAULT NULL,
  `mode` varchar(16) NOT NULL DEFAULT 'WEBHOOK',
  `status` tinyint NOT NULL DEFAULT 1,
  `remark` varchar(512) NULL DEFAULT NULL,
  `created_by` bigint NULL DEFAULT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_by` bigint NULL DEFAULT NULL,
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_tg_bot_no`(`bot_no` ASC) USING BTREE,
  UNIQUE INDEX `uk_tg_bot_token`(`token_hash` ASC) USING BTREE,
  INDEX `idx_tg_bot_tenant`(`tenant_id` ASC, `status` ASC) USING BTREE,
  CONSTRAINT `chk_tg_bot_scope` CHECK (`owner_scope` in ('PLATFORM','TENANT')),
  CONSTRAINT `chk_tg_bot_tenant` CHECK ((`owner_scope` <> 'TENANT') or (`tenant_id` is not null))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `tg_chat`;
CREATE TABLE `tg_chat` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NULL DEFAULT 0,
  `merchant_id` bigint NULL DEFAULT 0,
  `bot_id` bigint NOT NULL,
  `chat_id` bigint NOT NULL,
  `chat_type` varchar(16) NOT NULL,
  `title` varchar(255) NULL DEFAULT NULL,
  `purpose` varchar(16) NOT NULL DEFAULT 'NOTIFY',
  `event_types` varchar(512) NULL DEFAULT NULL,
  `lang` varchar(16) NOT NULL DEFAULT 'en-US',
  `status` tinyint NOT NULL DEFAULT 1,
  `remark` varchar(512) NULL DEFAULT NULL,
  `created_by` bigint NULL DEFAULT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_by` bigint NULL DEFAULT NULL,
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_tg_chat_bot_chat`(`bot_id` ASC, `chat_id` ASC) USING BTREE,
  INDEX `idx_tg_chat_tenant`(`tenant_id` ASC, `merchant_id` ASC, `status` ASC) USING BTREE,
  CONSTRAINT `chk_tg_chat_type` CHECK (`chat_type` in ('PRIVATE','GROUP','SUPERGROUP','CHANNEL'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `tg_message_task`;
CREATE TABLE `tg_message_task` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL DEFAULT 0,
  `merchant_id` bigint NOT NULL DEFAULT 0,
  `bot_id` bigint NOT NULL,
  `chat_id` bigint NOT NULL,
  `task_no` varchar(128) NOT NULL,
  `biz_type` varchar(64) NULL DEFAULT NULL,
  `biz_no` varchar(128) NULL DEFAULT NULL,
  `event_type` varchar(64) NULL DEFAULT NULL,
  `source_event_id` varchar(64) NULL DEFAULT NULL,
  `parse_mode` varchar(16) NOT NULL DEFAULT 'HTML',
  `content` text NOT NULL,
  `payload_json` json NULL,
  `status` varchar(32) NOT NULL DEFAULT 'INIT',
  `retry_count` int NOT NULL DEFAULT 0,
  `max_retry_count` int NOT NULL DEFAULT 8,
  `next_retry_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `tg_message_id` bigint NULL DEFAULT NULL,
  `last_error_code` int NULL DEFAULT NULL,
  `last_error_msg` varchar(1024) NULL DEFAULT NULL,
  `last_attempt_at` datetime(3) NULL DEFAULT NULL,
  `locked_by` varchar(128) NULL DEFAULT NULL,
  `lock_until` datetime(3) NULL DEFAULT NULL,
  `success_at` datetime(3) NULL DEFAULT NULL,
  `dead_at` datetime(3) NULL DEFAULT NULL,
  `trace_id` varchar(128) NULL DEFAULT NULL,
  `created_by` bigint NULL DEFAULT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_by` bigint NULL DEFAULT NULL,
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_tg_msg_task_no`(`tenant_id` ASC, `task_no` ASC) USING BTREE,
  UNIQUE INDEX `uk_tg_msg_source`(`bot_id` ASC, `chat_id` ASC, `source_event_id` ASC) USING BTREE,
  INDEX `idx_tg_msg_scan`(`status` ASC, `next_retry_at` ASC, `id` ASC) USING BTREE,
  INDEX `idx_tg_msg_lock`(`lock_until` ASC, `locked_by` ASC) USING BTREE,
  INDEX `idx_tg_msg_biz`(`tenant_id` ASC, `biz_type` ASC, `biz_no` ASC) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `tg_update_log`;
CREATE TABLE `tg_update_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `bot_id` bigint NOT NULL,
  `update_id` bigint NOT NULL,
  `tg_user_id` bigint NULL DEFAULT NULL,
  `chat_id` bigint NULL DEFAULT NULL,
  `update_type` varchar(32) NULL DEFAULT NULL,
  `command` varchar(64) NULL DEFAULT NULL,
  `raw_json` json NOT NULL,
  `handle_status` tinyint NOT NULL DEFAULT 0,
  `error_msg` varchar(1024) NULL DEFAULT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_tg_update`(`bot_id` ASC, `update_id` ASC) USING BTREE,
  INDEX `idx_tg_update_chat`(`chat_id` ASC, `created_at` ASC) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET FOREIGN_KEY_CHECKS = 1;
