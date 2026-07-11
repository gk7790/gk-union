SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `psp_callback_ip_whitelist`;
CREATE TABLE `psp_callback_ip_whitelist` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'primary key',
  `psp_code` varchar(64) NOT NULL COMMENT 'PSP code',
  `rule_name` varchar(128) NULL COMMENT 'rule name',
  `ip_pattern` varchar(128) NOT NULL COMMENT 'IPv4, CIDR, or *',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '1 normal, 2 pause, 3 stop',
  `remark` varchar(512) NULL COMMENT 'remark',
  `created_by` bigint NULL COMMENT 'created by',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_by` bigint NULL COMMENT 'updated by',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  INDEX `idx_psp_callback_ip_scope` (`psp_code`, `status`),
  INDEX `idx_psp_callback_ip_status` (`status`, `created_at`),
  CONSTRAINT `chk_psp_callback_ip_status` CHECK (`status` in (1,2,3))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='PSP callback IP whitelist';

SET FOREIGN_KEY_CHECKS = 1;
