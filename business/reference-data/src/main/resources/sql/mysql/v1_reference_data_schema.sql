-- gk-union MySQL schema generated from the provided MySQL dump.
-- Source: pasted-text.txt. Broken comments and Navicat metadata are intentionally omitted.

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `sys_bank`;
CREATE TABLE `sys_bank` (
  `id` bigint NOT NULL,
  `country_code` varchar(8) NOT NULL,
  `currency` varchar(8) NOT NULL,
  `bank_code` varchar(64) NOT NULL,
  `bank_name` varchar(255) NOT NULL,
  `bank_short_name` varchar(128) NULL DEFAULT NULL,
  `swift_code` varchar(32) NULL DEFAULT NULL,
  `local_clearing_code` varchar(64) NULL DEFAULT NULL,
  `status` tinyint NOT NULL DEFAULT 1,
  `sort` int NOT NULL DEFAULT 100,
  `remark` varchar(500) NULL DEFAULT NULL,
  `created_by` bigint NULL DEFAULT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_by` bigint NULL DEFAULT NULL,
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_country_currency_bank_code`(`bank_code` ASC, `country_code` ASC, `currency` ASC) USING BTREE,
  INDEX `idx_country_currency_status`(`country_code` ASC, `currency` ASC, `status` ASC) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `sys_currency`;
CREATE TABLE `sys_currency` (
  `id` bigint NOT NULL,
  `currency` varchar(8) NOT NULL,
  `currency_name` varchar(128) NOT NULL,
  `currency_symbol` varchar(16) NULL DEFAULT NULL,
  `numeric_code` varchar(8) NULL DEFAULT NULL,
  `minor_unit` tinyint NOT NULL DEFAULT 2,
  `status` tinyint NOT NULL DEFAULT 1,
  `sort` int NOT NULL DEFAULT 100,
  `remark` varchar(500) NULL DEFAULT NULL,
  `created_by` bigint NULL DEFAULT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_by` bigint NULL DEFAULT NULL,
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_currency`(`currency` ASC) USING BTREE,
  INDEX `idx_status_sort`(`status` ASC, `sort` ASC) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET FOREIGN_KEY_CHECKS = 1;
