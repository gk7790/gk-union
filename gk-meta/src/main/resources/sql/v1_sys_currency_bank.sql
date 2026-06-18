SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `sys_currency`;
CREATE TABLE `sys_currency` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `currency` varchar(8) NOT NULL COMMENT '币种代码，如 PHP、IDR、VND、USD',
  `currency_name` varchar(128) NOT NULL COMMENT '币种英文名称，如 Philippine Peso',
  `currency_symbol` varchar(16) DEFAULT NULL COMMENT '币种符号，如 ₱、Rp、₫、$',
  `numeric_code` varchar(8) DEFAULT NULL COMMENT 'ISO 4217数字代码，如 608、360、704',
  `minor_unit` tinyint NOT NULL DEFAULT '2' COMMENT '小数位，如 PHP=2，IDR=0，VND=0',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1启用 2暂停 3禁用',
  `sort` int NOT NULL DEFAULT '100' COMMENT '排序',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_currency` (`currency`),
  KEY `idx_status_sort` (`status`,`sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统币种字典表';

DROP TABLE IF EXISTS `sys_tenant_currency`;
CREATE TABLE `sys_tenant_currency` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `currency` varchar(8) NOT NULL COMMENT '币种代码，如 PHP、IDR、VND、USD',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1启用 2暂停 3禁用',
  `sort` int NOT NULL DEFAULT '100' COMMENT '排序',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_currency` (`tenant_id`,`currency`),
  KEY `idx_tenant_status` (`tenant_id`,`status`),
  KEY `idx_currency` (`currency`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='租户可用币种配置表';

DROP TABLE IF EXISTS `sys_bank`;
CREATE TABLE `sys_bank` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `country_code` varchar(8) NOT NULL COMMENT '国家代码，如 PH、ID、VN',
  `currency` varchar(8) NOT NULL COMMENT '币种，如 PHP、IDR、VND',
  `bank_code` varchar(64) NOT NULL COMMENT '平台标准银行编码，如 BDO、BPI、UNIONBANK',
  `bank_name` varchar(255) NOT NULL COMMENT '银行官方全称',
  `bank_short_name` varchar(128) DEFAULT NULL COMMENT '银行简称',
  `swift_code` varchar(32) DEFAULT NULL COMMENT 'SWIFT/BIC，可选',
  `local_clearing_code` varchar(64) DEFAULT NULL COMMENT '本地清算码，可选',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1启用 2暂停 3禁用',
  `sort` int NOT NULL DEFAULT '100' COMMENT '排序',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `created_by` bigint DEFAULT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_by` bigint DEFAULT NULL,
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_country_currency_bank_code` (`country_code`,`currency`,`bank_code`),
  KEY `idx_country_currency_status` (`country_code`,`currency`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='平台标准银行表';

DROP TABLE IF EXISTS `psp_bank_mapping`;
CREATE TABLE `psp_bank_mapping` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `psp_id` bigint NOT NULL COMMENT 'PSP供应商ID，关联 psp_provider.id',
  `psp_account_id` bigint DEFAULT NULL COMMENT 'PSP账户ID，可选；为空表示该PSP通用映射',
  `psp_method_id` bigint DEFAULT NULL COMMENT 'PSP方式ID，可选；通常对应 BANK',
  `country_code` varchar(8) NOT NULL COMMENT '国家代码，如 PH',
  `currency` varchar(8) NOT NULL COMMENT '币种，如 PHP',
  `bank_id` bigint NOT NULL COMMENT '平台标准银行ID，关联 sys_bank.id',
  `psp_bank_code` varchar(128) NOT NULL COMMENT 'PSP侧银行编码',
  `psp_bank_name` varchar(255) DEFAULT NULL COMMENT 'PSP侧银行名称',
  `psp_bank_short_name` varchar(128) DEFAULT NULL COMMENT 'PSP侧银行简称',
  `direction` varchar(16) DEFAULT NULL COMMENT '方向：PAYIN代收 / PAYOUT代付 / NULL通用',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1启用 2暂停 3禁用',
  `extra` json DEFAULT NULL COMMENT 'PSP额外配置，如branchCode、bankType等',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `created_by` bigint DEFAULT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_by` bigint DEFAULT NULL,
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_psp_account_method_bank_direction` (`psp_id`,`psp_account_id`,`psp_method_id`,`bank_id`,`direction`),
  KEY `idx_psp_country_currency` (`psp_id`,`country_code`,`currency`),
  KEY `idx_bank_id` (`bank_id`),
  KEY `idx_psp_bank_code` (`psp_id`,`psp_bank_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='PSP银行编码映射表';

SET FOREIGN_KEY_CHECKS = 1;
