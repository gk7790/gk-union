SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `psp_bank_mapping`;
CREATE TABLE `psp_bank_mapping` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `psp_id` bigint NOT NULL COMMENT 'PSP供应商ID，关联 psp_provider.id',
  `country_code` varchar(8) NOT NULL COMMENT '国家代码，如 PH',
  `currency` varchar(8) NOT NULL COMMENT '币种，如 PHP',
  `bank_code` varchar(64) NOT NULL COMMENT '平台标准银行编码，如 BDO、BPI、UBPH',
  `psp_bank_code` varchar(128) NOT NULL COMMENT 'PSP侧银行编码',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1启用 2暂停 3禁用',
  `sort` int NOT NULL DEFAULT '100' COMMENT '排序',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_psp_country_currency_bank` (`psp_id`,`country_code`,`currency`,`bank_code`),
  KEY `idx_psp_bank_code` (`psp_id`,`psp_bank_code`),
  KEY `idx_bank` (`bank_code`,`country_code`,`currency`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='PSP银行编码映射表';

SET FOREIGN_KEY_CHECKS = 1;
