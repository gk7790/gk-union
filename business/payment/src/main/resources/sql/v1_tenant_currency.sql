SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

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


SET FOREIGN_KEY_CHECKS = 1;
