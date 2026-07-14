-- gk-union MySQL schema generated from the provided MySQL dump.
-- Source: pasted-text.txt. Broken comments and Navicat metadata are intentionally omitted.

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `sys_dept`;
CREATE TABLE `sys_dept` (
  `id` bigint NOT NULL,
  `tenant_id` bigint NULL DEFAULT NULL,
  `pid` bigint NULL DEFAULT 0,
  `pids` varchar(500) NULL DEFAULT '',
  `name` varchar(50) NULL DEFAULT NULL,
  `status` tinyint NULL DEFAULT 1,
  `sort` int UNSIGNED NULL DEFAULT 0,
  `remark` varchar(255) NULL DEFAULT NULL,
  `created_by` bigint NULL DEFAULT NULL,
  `created_at` datetime NULL DEFAULT NULL,
  `updated_by` bigint NULL DEFAULT NULL,
  `updated_at` datetime NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_pid`(`pid` ASC) USING BTREE,
  INDEX `idx_sort`(`sort` ASC) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `sys_dict_data`;
CREATE TABLE `sys_dict_data` (
  `id` bigint NOT NULL,
  `tenant_id` bigint NULL DEFAULT NULL,
  `dict_type_id` bigint NOT NULL,
  `dict_label` varchar(255) NOT NULL,
  `i18n_key` varchar(255) NULL DEFAULT NULL,
  `dict_value` varchar(128) NULL DEFAULT NULL,
  `attr_type` varchar(255) NULL DEFAULT NULL,
  `remark` varchar(255) NULL DEFAULT NULL,
  `status` tinyint NULL DEFAULT 1,
  `sort` int UNSIGNED NULL DEFAULT NULL,
  `color` varchar(255) NULL DEFAULT NULL,
  `icon` varchar(255) NULL DEFAULT NULL,
  `ext` json NULL,
  `is_default` tinyint NULL DEFAULT NULL,
  `created_by` bigint NULL DEFAULT NULL,
  `created_at` datetime NULL DEFAULT NULL,
  `updated_by` bigint NULL DEFAULT NULL,
  `updated_at` datetime NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_dict_type_value`(`dict_type_id` ASC, `dict_value` ASC) USING BTREE,
  INDEX `idx_sort`(`sort` ASC) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `sys_dict_type`;
CREATE TABLE `sys_dict_type` (
  `id` bigint NOT NULL,
  `tenant_id` bigint NULL DEFAULT NULL,
  `dict_type` varchar(100) NOT NULL,
  `dict_name` varchar(255) NOT NULL,
  `remark` varchar(255) NULL DEFAULT NULL,
  `sort` int UNSIGNED NULL DEFAULT 0,
  `scope` varchar(255) NULL DEFAULT NULL,
  `created_by` bigint NULL DEFAULT NULL,
  `created_at` datetime NULL DEFAULT NULL,
  `updated_by` bigint NULL DEFAULT NULL,
  `updated_at` datetime NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `dict_type`(`dict_type` ASC) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `sys_i18n`;
CREATE TABLE `sys_i18n` (
  `id` bigint NOT NULL,
  `tenant_id` bigint NULL DEFAULT 0,
  `biz_type` varchar(64) NOT NULL,
  `biz_id` bigint NULL DEFAULT NULL,
  `i18n_key` varchar(128) NOT NULL,
  `lang` varchar(16) NOT NULL,
  `value` varchar(255) NULL DEFAULT NULL,
  `created_by` bigint NOT NULL,
  `created_at` datetime NOT NULL,
  `updated_by` bigint NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `sys_language`;
CREATE TABLE `sys_language` (
  `table_id` bigint NOT NULL,
  `table_name` varchar(32) NOT NULL,
  `field_name` varchar(32) NOT NULL,
  `field_value` varchar(200) NOT NULL,
  `language` varchar(10) NOT NULL,
  PRIMARY KEY (`table_id`, `table_name`, `field_name`, `language`) USING BTREE,
  INDEX `idx_table_id`(`table_id` ASC) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `sys_log_error`;
CREATE TABLE `sys_log_error` (
  `id` bigint NOT NULL,
  `trace_id` varchar(64) NULL DEFAULT NULL,
  `tenant_id` bigint NULL DEFAULT NULL,
  `user_id` bigint NULL DEFAULT NULL,
  `username` varchar(64) NULL DEFAULT NULL,
  `module` varchar(64) NULL DEFAULT NULL,
  `service_name` varchar(64) NULL DEFAULT NULL,
  `error_type` varchar(64) NULL DEFAULT NULL,
  `error_code` varchar(64) NULL DEFAULT NULL,
  `error_message` text NULL,
  `stack_trace` longtext NULL,
  `stack_hash` varchar(64) NULL DEFAULT NULL,
  `request_uri` varchar(500) NULL DEFAULT NULL,
  `request_method` varchar(16) NULL DEFAULT NULL,
  `request_params` longtext NULL,
  `request_body` longtext NULL,
  `request_headers` longtext NULL,
  `ip` varchar(64) NULL DEFAULT NULL,
  `country` varchar(64) NULL DEFAULT NULL,
  `province` varchar(64) NULL DEFAULT NULL,
  `city` varchar(64) NULL DEFAULT NULL,
  `user_agent` varchar(1000) NULL DEFAULT NULL,
  `device_type` varchar(32) NULL DEFAULT NULL,
  `os` varchar(64) NULL DEFAULT NULL,
  `browser` varchar(64) NULL DEFAULT NULL,
  `http_status` int NULL DEFAULT NULL,
  `level` varchar(16) NULL DEFAULT NULL,
  `resolved` tinyint NULL DEFAULT 0,
  `alarmed` tinyint NULL DEFAULT 0,
  `env` varchar(32) NULL DEFAULT NULL,
  `created_at` datetime NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_trace_id`(`trace_id` ASC) USING BTREE,
  INDEX `idx_tenant_id`(`tenant_id` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_error_type`(`error_type` ASC) USING BTREE,
  INDEX `idx_stack_hash`(`stack_hash` ASC) USING BTREE,
  INDEX `idx_created_at`(`created_at` ASC) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `sys_log_login`;
CREATE TABLE `sys_log_login` (
  `id` bigint NOT NULL,
  `operation` tinyint UNSIGNED NULL DEFAULT NULL,
  `status` tinyint UNSIGNED NOT NULL,
  `user_agent` varchar(500) NULL DEFAULT NULL,
  `ip` varchar(32) NULL DEFAULT NULL,
  `created_by_name` varchar(50) NULL DEFAULT NULL,
  `created_by` bigint NULL DEFAULT NULL,
  `created_at` datetime NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_created_at`(`created_at` ASC) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `sys_log_operation`;
CREATE TABLE `sys_log_operation` (
  `id` bigint NOT NULL,
  `operation` varchar(50) NULL DEFAULT NULL,
  `request_uri` varchar(200) NULL DEFAULT NULL,
  `request_method` varchar(20) NULL DEFAULT NULL,
  `request_params` mediumtext NULL,
  `request_time` int UNSIGNED NOT NULL,
  `user_agent` varchar(500) NULL DEFAULT NULL,
  `ip` varchar(32) NULL DEFAULT NULL,
  `status` tinyint UNSIGNED NOT NULL,
  `created_by_name` varchar(50) NULL DEFAULT NULL,
  `created_by` bigint NULL DEFAULT NULL,
  `created_at` datetime NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_created_at`(`created_at` ASC) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `sys_menu`;
CREATE TABLE `sys_menu` (
  `id` bigint NOT NULL,
  `pid` bigint NULL DEFAULT 0,
  `name` varchar(255) NULL DEFAULT NULL,
  `path` varchar(200) NULL DEFAULT NULL,
  `type` varchar(255) NULL DEFAULT NULL,
  `status` tinyint NULL DEFAULT 1,
  `auth_code` varchar(500) NULL DEFAULT NULL,
  `active_path` varchar(255) NULL DEFAULT NULL,
  `component` varchar(255) NULL DEFAULT NULL,
  `meta` json NULL,
  `sort` int NULL DEFAULT 0,
  `redirect` varchar(255) NULL DEFAULT NULL,
  `subject_types` json NULL,
  `domain` json NULL,
  `created_by` bigint NULL DEFAULT NULL,
  `created_at` datetime NULL DEFAULT NULL,
  `updated_by` bigint NULL DEFAULT NULL,
  `updated_at` datetime NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `unq_name`(`name` ASC) USING BTREE,
  INDEX `idx_pid`(`pid` ASC) USING BTREE,
  INDEX `idx_sort`(`sort` ASC) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `sys_params`;
CREATE TABLE `sys_params` (
  `id` bigint NOT NULL,
  `param_code` varchar(32) NULL DEFAULT NULL,
  `param_value` varchar(2000) NULL DEFAULT NULL,
  `param_type` tinyint UNSIGNED NULL DEFAULT 1,
  `remark` varchar(200) NULL DEFAULT NULL,
  `created_by` bigint NULL DEFAULT NULL,
  `created_at` datetime NULL DEFAULT NULL,
  `updated_by` bigint NULL DEFAULT NULL,
  `updated_at` datetime NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_param_code`(`param_code` ASC) USING BTREE,
  INDEX `idx_created_at`(`created_at` ASC) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `sys_region`;
CREATE TABLE `sys_region` (
  `id` bigint NOT NULL,
  `pid` bigint NULL DEFAULT NULL,
  `name` varchar(100) NULL DEFAULT NULL,
  `tree_level` tinyint NULL DEFAULT NULL,
  `leaf` tinyint NULL DEFAULT NULL,
  `sort` bigint NULL DEFAULT NULL,
  `created_by` bigint NULL DEFAULT NULL,
  `created_at` datetime NULL DEFAULT NULL,
  `updated_by` bigint NULL DEFAULT NULL,
  `updated_at` datetime NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role` (
  `id` bigint NOT NULL,
  `tenant_id` bigint NOT NULL DEFAULT 0,
  `dept_id` bigint NOT NULL DEFAULT 0,
  `name` varchar(50) NULL DEFAULT NULL,
  `auth` varchar(255) NULL DEFAULT NULL,
  `data_scope` tinyint NULL DEFAULT NULL,
  `role_scope` varchar(255) NULL DEFAULT NULL,
  `status` tinyint NULL DEFAULT 1,
  `remark` varchar(100) NULL DEFAULT NULL,
  `created_by` bigint NULL DEFAULT NULL,
  `created_at` datetime NULL DEFAULT NULL,
  `updated_by` bigint NULL DEFAULT NULL,
  `updated_at` datetime NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `unq_auth`(`tenant_id` ASC, `auth` ASC) USING BTREE,
  INDEX `idx_dept_id`(`dept_id` ASC) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `sys_role_data_scope`;
CREATE TABLE `sys_role_data_scope` (
  `id` bigint NOT NULL,
  `role_id` bigint NULL DEFAULT NULL,
  `dept_id` bigint NULL DEFAULT NULL,
  `created_by` bigint NULL DEFAULT NULL,
  `created_at` datetime NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_role_id`(`role_id` ASC) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `sys_role_menu`;
CREATE TABLE `sys_role_menu` (
  `id` bigint NOT NULL,
  `role_id` bigint NULL DEFAULT NULL,
  `menu_id` bigint NULL DEFAULT NULL,
  `created_by` bigint NULL DEFAULT NULL,
  `created_at` datetime NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_role_id`(`role_id` ASC) USING BTREE,
  INDEX `idx_menu_id`(`menu_id` ASC) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `sys_role_user`;
CREATE TABLE `sys_role_user` (
  `id` bigint NOT NULL,
  `user_subject_id` bigint NULL DEFAULT NULL,
  `role_id` bigint NULL DEFAULT NULL,
  `user_id` bigint NULL DEFAULT NULL,
  `created_by` bigint NULL DEFAULT NULL,
  `created_at` datetime NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_role_id`(`role_id` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `sys_tenant`;
CREATE TABLE `sys_tenant` (
  `id` bigint NULL DEFAULT NULL,
  `name` varchar(255) NULL DEFAULT NULL,
  `code` varchar(100) NULL DEFAULT NULL,
  `status` tinyint NULL DEFAULT 1,
  `domain` varchar(255) NULL DEFAULT NULL,
  `currency` varchar(20) NULL DEFAULT NULL,
  `timezone` varchar(50) NULL DEFAULT 'UTC',
  `lang` varchar(255) NULL DEFAULT 'en-US',
  `api_key` varchar(255) NULL DEFAULT NULL,
  `api_secret` varchar(255) NULL DEFAULT NULL,
  `remark` varchar(255) NULL DEFAULT NULL,
  `created_by` bigint NULL DEFAULT NULL,
  `created_at` datetime NULL DEFAULT NULL,
  `updated_by` bigint NULL DEFAULT NULL,
  `updated_at` datetime NULL DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
  `id` bigint NOT NULL,
  `nickname` varchar(255) NULL DEFAULT NULL,
  `username` varchar(50) NOT NULL,
  `password` varchar(100) NOT NULL,
  `real_name` varchar(50) NULL DEFAULT NULL,
  `avatar` varchar(200) NULL DEFAULT NULL,
  `gender` tinyint UNSIGNED NULL DEFAULT NULL,
  `email` varchar(100) NULL DEFAULT NULL,
  `mobile` varchar(100) NULL DEFAULT NULL,
  `status` tinyint NOT NULL DEFAULT 1,
  `remark` varchar(255) NULL DEFAULT NULL,
  `created_by` bigint NULL DEFAULT NULL,
  `created_at` datetime NULL DEFAULT NULL,
  `updated_by` bigint NULL DEFAULT NULL,
  `updated_at` datetime NULL DEFAULT NULL,
  `auth_type` tinyint NULL DEFAULT 1,
  `auth_secret` varchar(255) NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_username`(`username` ASC) USING BTREE,
  UNIQUE INDEX `uk_sys_user_mobile`(`mobile` ASC) USING BTREE,
  UNIQUE INDEX `uk_sys_user_email`(`email` ASC) USING BTREE,
  INDEX `idx_created_at`(`created_at` ASC) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `sys_user_subject`;
CREATE TABLE `sys_user_subject` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `subject_type` varchar(32) NOT NULL,
  `tenant_id` bigint NULL DEFAULT NULL,
  `merchant_id` bigint NULL DEFAULT NULL,
  `dept_id` bigint NULL DEFAULT NULL,
  `status` tinyint NOT NULL DEFAULT 1,
  `remark` varchar(512) NULL DEFAULT NULL,
  `created_by` bigint NULL DEFAULT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_by` bigint NULL DEFAULT NULL,
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_sys_user_subject_user`(`user_id` ASC) USING BTREE,
  INDEX `idx_sys_user_subject_type`(`subject_type` ASC, `status` ASC) USING BTREE,
  INDEX `idx_sys_user_subject_tenant`(`tenant_id` ASC, `status` ASC) USING BTREE,
  INDEX `idx_sys_user_subject_merchant`(`tenant_id` ASC, `merchant_id` ASC, `status` ASC) USING BTREE,
  CONSTRAINT `chk_sys_user_subject_status` CHECK (`status` in (0,1)),
  CONSTRAINT `chk_sys_user_subject_tenant` CHECK ((`subject_type` <> 'TENANT') or ((`tenant_id` is not null) and (`merchant_id` is null))),
  CONSTRAINT `chk_sys_user_subject_type` CHECK (`subject_type` in ('PLATFORM','TENANT','MERCHANT'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET FOREIGN_KEY_CHECKS = 1;
