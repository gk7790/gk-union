SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `sys_api_ip_whitelist`;
DROP TABLE IF EXISTS `merchant_api_ip_whitelist`;
CREATE TABLE `merchant_api_ip_whitelist` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'primary key',
  `tenant_id` bigint NOT NULL COMMENT 'tenant id',
  `merchant_id` bigint NOT NULL COMMENT 'merchant id',
  `rule_name` varchar(128) NULL COMMENT 'rule name',
  `ip_pattern` varchar(128) NOT NULL COMMENT 'IPv4, CIDR, or *',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '1 normal, 2 pause, 3 stop',
  `remark` varchar(512) NULL COMMENT 'remark',
  `created_by` bigint NULL COMMENT 'created by',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_by` bigint NULL COMMENT 'updated by',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  INDEX `idx_merchant_api_ip_scope` (`tenant_id`, `merchant_id`, `status`),
  INDEX `idx_merchant_api_ip_status` (`status`, `created_at`),
  CONSTRAINT `chk_merchant_api_ip_status` CHECK (`status` in (1,2,3))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='merchant API IP whitelist';

SET FOREIGN_KEY_CHECKS = 1;
