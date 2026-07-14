-- gk-union MySQL schema generated from the provided MySQL dump.
-- Source: pasted-text.txt. Broken comments and Navicat metadata are intentionally omitted.

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `psp_account`;
CREATE TABLE `psp_account` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `psp_id` bigint NOT NULL,
  `psp_account_no` varchar(128) NOT NULL,
  `psp_account_name` varchar(128) NULL DEFAULT NULL,
  `status` tinyint NOT NULL DEFAULT 1,
  `secret_type` varchar(32) NOT NULL DEFAULT 'HMAC',
  `api_key` varchar(256) NULL DEFAULT NULL,
  `api_secret` varchar(512) NULL DEFAULT NULL,
  `callback_secret` varchar(512) NULL DEFAULT NULL,
  `config_json` json NULL,
  `remark` varchar(512) NULL DEFAULT NULL,
  `created_by` bigint NULL DEFAULT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_by` bigint NULL DEFAULT NULL,
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_psp_account_no`(`psp_account_no` ASC) USING BTREE,
  INDEX `idx_psp_account_tenant`(`tenant_id` ASC, `psp_id` ASC, `status` ASC) USING BTREE,
  INDEX `idx_psp_account_no`(`psp_id` ASC, `psp_account_no` ASC) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `psp_bank_mapping`;
CREATE TABLE `psp_bank_mapping` (
  `id` bigint NOT NULL,
  `psp_id` bigint NOT NULL,
  `country_code` varchar(8) NOT NULL,
  `currency` varchar(8) NOT NULL,
  `bank_code` varchar(64) NOT NULL,
  `psp_bank_code` varchar(128) NOT NULL,
  `status` tinyint NOT NULL DEFAULT 1,
  `sort` int NOT NULL DEFAULT 100,
  `remark` varchar(500) NULL DEFAULT NULL,
  `created_by` bigint NULL DEFAULT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_by` bigint NULL DEFAULT NULL,
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_psp_country_currency_bank`(`psp_id` ASC, `country_code` ASC, `currency` ASC, `bank_code` ASC) USING BTREE,
  INDEX `idx_psp_bank_code`(`psp_id` ASC, `psp_bank_code` ASC) USING BTREE,
  INDEX `idx_bank`(`bank_code` ASC, `country_code` ASC, `currency` ASC) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `psp_callback_ip_whitelist`;
CREATE TABLE `psp_callback_ip_whitelist` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `psp_code` varchar(64) NOT NULL,
  `rule_name` varchar(128) NULL DEFAULT NULL,
  `ip_pattern` varchar(128) NOT NULL,
  `status` tinyint NOT NULL DEFAULT 1,
  `remark` varchar(512) NULL DEFAULT NULL,
  `created_by` bigint NULL DEFAULT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_by` bigint NULL DEFAULT NULL,
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_psp_callback_ip_scope`(`psp_code` ASC, `status` ASC) USING BTREE,
  INDEX `idx_psp_callback_ip_status`(`status` ASC, `created_at` ASC) USING BTREE,
  CONSTRAINT `chk_psp_callback_ip_status` CHECK (`status` in (1,2,3))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `psp_callback_log`;
CREATE TABLE `psp_callback_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `merchant_id` bigint NULL DEFAULT NULL,
  `psp_id` bigint NOT NULL,
  `psp_code` varchar(64) NOT NULL,
  `biz_type` varchar(64) NULL DEFAULT NULL,
  `biz_id` bigint NULL DEFAULT NULL,
  `biz_no` varchar(128) NULL DEFAULT NULL,
  `psp_order_no` varchar(128) NULL DEFAULT NULL,
  `callback_type` varchar(64) NOT NULL,
  `callback_id` varchar(128) NULL DEFAULT NULL,
  `callback_key` varchar(256) NOT NULL,
  `body_hash` varchar(128) NOT NULL,
  `headers_json` json NULL,
  `body_json` json NULL,
  `raw_body` text NULL,
  `signature` varchar(512) NULL DEFAULT NULL,
  `verify_status` varchar(32) NOT NULL DEFAULT 'INIT',
  `process_status` varchar(32) NOT NULL DEFAULT 'INIT',
  `error_msg` varchar(1024) NULL DEFAULT NULL,
  `received_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `processed_at` datetime(3) NULL DEFAULT NULL,
  `trace_id` varchar(128) NULL DEFAULT NULL,
  `created_by` bigint NULL DEFAULT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_by` bigint NULL DEFAULT NULL,
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_psp_callback_key`(`psp_id` ASC, `callback_key` ASC) USING BTREE,
  INDEX `idx_psp_callback_biz`(`tenant_id` ASC, `biz_type` ASC, `biz_no` ASC) USING BTREE,
  INDEX `idx_psp_callback_order`(`psp_id` ASC, `psp_order_no` ASC) USING BTREE,
  INDEX `idx_psp_callback_process`(`process_status` ASC, `received_at` ASC) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `psp_fee_rule`;
CREATE TABLE `psp_fee_rule` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `psp_id` bigint NOT NULL,
  `psp_account_id` bigint NULL DEFAULT NULL,
  `psp_method_id` bigint NULL DEFAULT NULL,
  `method_code` varchar(64) NOT NULL,
  `psp_method_code` varchar(64) NOT NULL,
  `rule_name` varchar(128) NOT NULL,
  `direction` varchar(16) NOT NULL,
  `country_code` varchar(8) NULL DEFAULT NULL,
  `currency` varchar(16) NOT NULL,
  `min_amount` decimal(24, 8) NOT NULL DEFAULT 0.00000000,
  `max_amount` decimal(24, 8) NOT NULL DEFAULT 0.00000000,
  `fee_mode` varchar(32) NOT NULL,
  `fee_rate` decimal(18, 8) NOT NULL DEFAULT 0.00000000,
  `fee_fixed` decimal(24, 8) NOT NULL DEFAULT 0.00000000,
  `min_fee` decimal(24, 8) NOT NULL DEFAULT 0.00000000,
  `max_fee` decimal(24, 8) NOT NULL DEFAULT 0.00000000,
  `priority` int NOT NULL DEFAULT 100,
  `effective_at` datetime(3) NULL DEFAULT NULL,
  `expire_at` datetime(3) NULL DEFAULT NULL,
  `status` tinyint NOT NULL DEFAULT 1,
  `remark` varchar(512) NULL DEFAULT NULL,
  `created_by` bigint NULL DEFAULT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_by` bigint NULL DEFAULT NULL,
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_psp_fee_rule_psp`(`tenant_id` ASC, `psp_id` ASC, `direction` ASC, `currency` ASC, `status` ASC) USING BTREE,
  INDEX `idx_psp_fee_rule_account`(`tenant_id` ASC, `psp_account_id` ASC, `direction` ASC, `currency` ASC, `status` ASC) USING BTREE,
  INDEX `idx_psp_fee_rule_method`(`tenant_id` ASC, `psp_method_id` ASC, `direction` ASC, `currency` ASC, `status` ASC) USING BTREE,
  INDEX `idx_psp_fee_rule_match`(`tenant_id` ASC, `psp_id` ASC, `psp_account_id` ASC, `psp_method_id` ASC, `country_code` ASC, `currency` ASC, `method_code` ASC, `direction` ASC, `status` ASC, `priority` ASC) USING BTREE,
  INDEX `idx_psp_fee_rule_effective`(`tenant_id` ASC, `status` ASC, `effective_at` ASC, `expire_at` ASC) USING BTREE,
  INDEX `idx_psp_fee_rule_plan_match`(`tenant_id` ASC, `psp_id` ASC, `psp_account_id` ASC, `psp_method_id` ASC, `currency` ASC, `method_code` ASC, `direction` ASC, `status` ASC, `priority` ASC, `id` ASC) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `psp_method`;
CREATE TABLE `psp_method` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `psp_id` bigint NOT NULL,
  `psp_code` varchar(64) NOT NULL,
  `method_code` varchar(64) NOT NULL,
  `psp_method_code` varchar(128) NOT NULL,
  `method_name` varchar(128) NULL DEFAULT NULL,
  `country_code` varchar(8) NOT NULL,
  `currency` varchar(16) NOT NULL,
  `direction` varchar(16) NOT NULL,
  `min_amount` decimal(24, 8) NOT NULL DEFAULT 0.00000000,
  `max_amount` decimal(24, 8) NOT NULL DEFAULT 0.00000000,
  `daily_limit` decimal(24, 8) NULL DEFAULT NULL,
  `status` tinyint NOT NULL DEFAULT 1,
  `config_json` json NULL,
  `remark` varchar(512) NULL DEFAULT NULL,
  `created_by` bigint NULL DEFAULT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_by` bigint NULL DEFAULT NULL,
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_psp_method_scope`(`psp_id` ASC, `country_code` ASC, `currency` ASC, `method_code` ASC, `direction` ASC) USING BTREE,
  INDEX `idx_psp_method_match`(`country_code` ASC, `currency` ASC, `method_code` ASC, `direction` ASC, `status` ASC) USING BTREE,
  INDEX `idx_psp_method_psp`(`psp_id` ASC, `status` ASC) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `psp_provider`;
CREATE TABLE `psp_provider` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `psp_code` varchar(64) NOT NULL,
  `psp_name` varchar(128) NOT NULL,
  `country_code` varchar(8) NULL DEFAULT NULL,
  `status` tinyint NOT NULL DEFAULT 1,
  `base_url` varchar(512) NULL DEFAULT NULL,
  `api_version` varchar(32) NULL DEFAULT NULL,
  `support_payin` tinyint NOT NULL DEFAULT 1,
  `support_payout` tinyint NOT NULL DEFAULT 1,
  `config_json` json NULL,
  `remark` varchar(512) NULL DEFAULT NULL,
  `created_by` bigint NULL DEFAULT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_by` bigint NULL DEFAULT NULL,
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_psp_provider_tenant_code`(`tenant_id` ASC, `psp_code` ASC) USING BTREE,
  INDEX `idx_psp_provider_country_status`(`tenant_id` ASC, `country_code` ASC, `status` ASC) USING BTREE,
  INDEX `idx_psp_provider_status`(`tenant_id` ASC, `status` ASC) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `psp_request_log`;
CREATE TABLE `psp_request_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `merchant_id` bigint NULL DEFAULT NULL,
  `psp_id` bigint NOT NULL,
  `psp_code` varchar(64) NOT NULL,
  `biz_type` varchar(64) NOT NULL,
  `biz_id` bigint NULL DEFAULT NULL,
  `biz_no` varchar(128) NOT NULL,
  `request_no` varchar(128) NOT NULL,
  `psp_request_no` varchar(128) NULL DEFAULT NULL,
  `psp_order_no` varchar(128) NULL DEFAULT NULL,
  `request_url` varchar(1024) NULL DEFAULT NULL,
  `http_method` varchar(16) NULL DEFAULT NULL,
  `request_headers_json` json NULL,
  `request_body` text NULL,
  `response_status` int NULL DEFAULT NULL,
  `response_body` text NULL,
  `success` tinyint NULL DEFAULT NULL,
  `error_code` varchar(128) NULL DEFAULT NULL,
  `error_msg` varchar(1024) NULL DEFAULT NULL,
  `cost_ms` bigint NULL DEFAULT NULL,
  `trace_id` varchar(128) NULL DEFAULT NULL,
  `created_by` bigint NULL DEFAULT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_by` bigint NULL DEFAULT NULL,
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_psp_request_no`(`request_no` ASC) USING BTREE,
  INDEX `idx_psp_request_biz`(`tenant_id` ASC, `biz_type` ASC, `biz_no` ASC) USING BTREE,
  INDEX `idx_psp_request_psp_order`(`psp_id` ASC, `psp_order_no` ASC) USING BTREE,
  INDEX `idx_psp_request_created`(`tenant_id` ASC, `created_at` ASC) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET FOREIGN_KEY_CHECKS = 1;
