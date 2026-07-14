-- gk-union MySQL schema generated from the provided MySQL dump.
-- Source: pasted-text.txt. Broken comments and Navicat metadata are intentionally omitted.

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `gen_base_class`;
CREATE TABLE `gen_base_class` (
  `id` bigint NOT NULL,
  `package_name` varchar(200) NULL DEFAULT NULL,
  `code` varchar(200) NULL DEFAULT NULL,
  `fields` varchar(500) NULL DEFAULT NULL,
  `remark` varchar(200) NULL DEFAULT NULL,
  `created_at` datetime NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `gen_datasource`;
CREATE TABLE `gen_datasource` (
  `id` bigint NOT NULL,
  `db_type` varchar(200) NULL DEFAULT NULL,
  `conn_name` varchar(200) NOT NULL,
  `conn_url` varchar(500) NULL DEFAULT NULL,
  `username` varchar(200) NULL DEFAULT NULL,
  `password` varchar(200) NULL DEFAULT NULL,
  `status` tinyint NULL DEFAULT NULL,
  `created_at` datetime NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `gen_field_type`;
CREATE TABLE `gen_field_type` (
  `id` bigint NOT NULL,
  `column_type` varchar(128) NULL DEFAULT NULL,
  `attr_type` varchar(200) NULL DEFAULT NULL,
  `ui_type` varchar(255) NULL DEFAULT NULL,
  `package_name` varchar(200) NULL DEFAULT NULL,
  `created_at` datetime NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `column_type`(`column_type` ASC) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `gen_table_field`;
CREATE TABLE `gen_table_field` (
  `id` bigint NOT NULL,
  `table_id` bigint NULL DEFAULT NULL,
  `table_name` varchar(200) NULL DEFAULT NULL,
  `column_name` varchar(200) NULL DEFAULT NULL,
  `column_type` varchar(200) NULL DEFAULT NULL,
  `column_comment` varchar(200) NULL DEFAULT NULL,
  `attr_name` varchar(200) NULL DEFAULT NULL,
  `attr_type` varchar(200) NULL DEFAULT NULL,
  `ui_type` varchar(255) NULL DEFAULT NULL,
  `package_name` varchar(200) NULL DEFAULT NULL,
  `is_pk` tinyint NULL DEFAULT NULL,
  `is_required` tinyint NULL DEFAULT NULL,
  `is_form` tinyint NULL DEFAULT NULL,
  `is_list` tinyint NULL DEFAULT NULL,
  `is_query` tinyint NULL DEFAULT NULL,
  `query_type` varchar(200) NULL DEFAULT NULL,
  `form_type` varchar(200) NULL DEFAULT NULL,
  `dict_name` varchar(200) NULL DEFAULT NULL,
  `validator_type` varchar(200) NULL DEFAULT NULL,
  `sort` int NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `table_name`(`table_name` ASC) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `gen_table_info`;
CREATE TABLE `gen_table_info` (
  `id` bigint NOT NULL,
  `table_name` varchar(128) NULL DEFAULT NULL,
  `class_name` varchar(200) NULL DEFAULT NULL,
  `table_comment` varchar(200) NULL DEFAULT NULL,
  `author` varchar(200) NULL DEFAULT NULL,
  `email` varchar(200) NULL DEFAULT NULL,
  `package_name` varchar(200) NULL DEFAULT NULL,
  `version` varchar(200) NULL DEFAULT NULL,
  `backend_path` varchar(500) NULL DEFAULT NULL,
  `frontend_path` varchar(500) NULL DEFAULT NULL,
  `module_name` varchar(200) NULL DEFAULT NULL,
  `sub_module_name` varchar(200) NULL DEFAULT NULL,
  `datasource_id` bigint NULL DEFAULT NULL,
  `baseclass_id` bigint NULL DEFAULT NULL,
  `created_at` datetime NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `table_name`(`table_name` ASC) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `gen_template`;
CREATE TABLE `gen_template` (
  `id` bigint NOT NULL,
  `name` varchar(200) NULL DEFAULT NULL,
  `file_name` varchar(200) NULL DEFAULT NULL,
  `content` mediumtext NOT NULL,
  `path` varchar(500) NULL DEFAULT NULL,
  `status` tinyint NULL DEFAULT NULL,
  `created_at` datetime NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET FOREIGN_KEY_CHECKS = 1;
