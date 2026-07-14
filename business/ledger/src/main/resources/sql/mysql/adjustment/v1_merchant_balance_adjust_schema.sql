-- gk-union MySQL schema generated from the provided MySQL dump.
-- Source: pasted-text.txt. Broken comments and Navicat metadata are intentionally omitted.

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `merchant_balance_adjust_order`;
CREATE TABLE `merchant_balance_adjust_order` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `merchant_id` bigint NOT NULL,
  `merchant_no` varchar(64) NOT NULL,
  `adjust_order_no` varchar(128) NOT NULL,
  `adjust_type` varchar(32) NOT NULL,
  `source_type` varchar(32) NOT NULL DEFAULT 'MANUAL',
  `currency` varchar(16) NOT NULL,
  `amount` decimal(24, 8) NOT NULL,
  `status` varchar(32) NOT NULL DEFAULT 'CREATED',
  `reason` varchar(512) NULL DEFAULT NULL,
  `related_order_no` varchar(128) NULL DEFAULT NULL,
  `reverse_of_journal_no` varchar(64) NULL DEFAULT NULL,
  `ledger_journal_no` varchar(64) NULL DEFAULT NULL,
  `trace_id` varchar(128) NULL DEFAULT NULL,
  `posted_at` datetime(3) NULL DEFAULT NULL,
  `operator_type` varchar(32) NOT NULL DEFAULT 'ADMIN',
  `operator_id` varchar(128) NULL DEFAULT NULL,
  `extra_json` json NULL,
  `version` int NOT NULL DEFAULT 0,
  `remark` varchar(512) NULL DEFAULT NULL,
  `created_by` bigint NULL DEFAULT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_by` bigint NULL DEFAULT NULL,
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_mba_order_no`(`tenant_id` ASC, `adjust_order_no` ASC) USING BTREE,
  INDEX `idx_mba_merchant_created`(`tenant_id` ASC, `merchant_id` ASC, `created_at` ASC) USING BTREE,
  INDEX `idx_mba_type_status`(`tenant_id` ASC, `adjust_type` ASC, `status` ASC, `created_at` ASC) USING BTREE,
  INDEX `idx_mba_journal`(`tenant_id` ASC, `ledger_journal_no` ASC) USING BTREE,
  INDEX `idx_mba_related_order`(`tenant_id` ASC, `related_order_no` ASC) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET FOREIGN_KEY_CHECKS = 1;
