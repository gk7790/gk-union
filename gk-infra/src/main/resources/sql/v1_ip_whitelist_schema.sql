SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `sys_login_ip_whitelist`;
CREATE TABLE `sys_login_ip_whitelist` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'primary key',
  `subject_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'login subject type: PLATFORM/TENANT/MERCHANT',
  `tenant_id` bigint NULL DEFAULT NULL COMMENT 'tenant id, null means no tenant restriction',
  `merchant_id` bigint NULL DEFAULT NULL COMMENT 'merchant id, null means no merchant restriction',
  `subject_id` bigint NULL DEFAULT NULL COMMENT 'sys_user_subject id, null means all subjects in the scope',
  `rule_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT 'rule name',
  `ip_pattern` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'IPv4, CIDR, or *',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '1 normal, 2 pause, 3 stop',
  `remark` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT 'remark',
  `created_by` bigint NULL DEFAULT NULL COMMENT 'created by',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT 'created at',
  `updated_by` bigint NULL DEFAULT NULL COMMENT 'updated by',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT 'updated at',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_login_ip_subject` (`subject_type`, `tenant_id`, `merchant_id`, `subject_id`, `status`) USING BTREE,
  INDEX `idx_login_ip_status` (`status`, `created_at`) USING BTREE,
  CONSTRAINT `chk_login_ip_subject_type` CHECK (`subject_type` in ('PLATFORM','TENANT','MERCHANT')),
  CONSTRAINT `chk_login_ip_status` CHECK (`status` in (1,2,3))
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'login IP whitelist' ROW_FORMAT = Dynamic;

DROP TABLE IF EXISTS `sys_api_ip_whitelist`;
CREATE TABLE `sys_api_ip_whitelist` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'primary key',
  `api_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'MERCHANT_OPENAPI' COMMENT 'api type',
  `tenant_id` bigint NOT NULL COMMENT 'tenant id',
  `merchant_id` bigint NOT NULL COMMENT 'merchant id',
  `rule_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT 'rule name',
  `ip_pattern` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'IPv4, CIDR, or *',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '1 normal, 2 pause, 3 stop',
  `remark` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT 'remark',
  `created_by` bigint NULL DEFAULT NULL COMMENT 'created by',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT 'created at',
  `updated_by` bigint NULL DEFAULT NULL COMMENT 'updated by',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT 'updated at',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_api_ip_merchant` (`api_type`, `tenant_id`, `merchant_id`, `status`) USING BTREE,
  INDEX `idx_api_ip_status` (`status`, `created_at`) USING BTREE,
  CONSTRAINT `chk_api_ip_type` CHECK (`api_type` in ('MERCHANT_OPENAPI')),
  CONSTRAINT `chk_api_ip_status` CHECK (`status` in (1,2,3))
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'API IP whitelist' ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
