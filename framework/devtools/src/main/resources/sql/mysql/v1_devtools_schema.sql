-- gk-union MySQL schema generated from the provided MySQL dump.
-- Source: pasted-text.txt. Navicat metadata is intentionally omitted.

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `gen_base_class`;
CREATE TABLE `gen_base_class` (
  `id` bigint NOT NULL COMMENT 'id',
  `package_name` varchar(200) NULL DEFAULT NULL COMMENT '基类包名',
  `code` varchar(200) NULL DEFAULT NULL COMMENT '基类编码',
  `fields` varchar(500) NULL DEFAULT NULL COMMENT '基类字段，多个用英文逗号分隔',
  `remark` varchar(200) NULL DEFAULT NULL COMMENT '备注',
  `created_at` datetime NULL DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='基类管理';

DROP TABLE IF EXISTS `gen_datasource`;
CREATE TABLE `gen_datasource` (
  `id` bigint NOT NULL COMMENT 'id',
  `db_type` varchar(200) NULL DEFAULT NULL COMMENT '数据库类型 MySQL、Oracle、SQLServer、PostgreSQL',
  `conn_name` varchar(200) NOT NULL COMMENT '连接名',
  `conn_url` varchar(500) NULL DEFAULT NULL COMMENT 'URL',
  `username` varchar(200) NULL DEFAULT NULL COMMENT '用户名',
  `password` varchar(200) NULL DEFAULT NULL COMMENT '密码',
  `status` tinyint NULL DEFAULT NULL COMMENT '状态: 1正常 2暂停 3停用',
  `created_at` datetime NULL DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据源管理';

DROP TABLE IF EXISTS `gen_field_type`;
CREATE TABLE `gen_field_type` (
  `id` bigint NOT NULL COMMENT 'id',
  `column_type` varchar(128) NULL DEFAULT NULL COMMENT '字段类型',
  `attr_type` varchar(200) NULL DEFAULT NULL COMMENT '属性类型',
  `ui_type` varchar(255) NULL DEFAULT NULL COMMENT 'ta字段类型',
  `package_name` varchar(200) NULL DEFAULT NULL COMMENT '属性包名',
  `created_at` datetime NULL DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `column_type`(`column_type` ASC) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='字段类型管理';

DROP TABLE IF EXISTS `gen_table_field`;
CREATE TABLE `gen_table_field` (
  `id` bigint NOT NULL COMMENT 'id',
  `table_id` bigint NULL DEFAULT NULL COMMENT '表ID',
  `table_name` varchar(200) NULL DEFAULT NULL COMMENT '表名',
  `column_name` varchar(200) NULL DEFAULT NULL COMMENT '列名',
  `column_type` varchar(200) NULL DEFAULT NULL COMMENT '类型',
  `column_comment` varchar(200) NULL DEFAULT NULL COMMENT '列说明',
  `attr_name` varchar(200) NULL DEFAULT NULL COMMENT '属性名',
  `attr_type` varchar(200) NULL DEFAULT NULL COMMENT '属性类型',
  `ui_type` varchar(255) NULL DEFAULT NULL COMMENT 'Ts属性类型',
  `package_name` varchar(200) NULL DEFAULT NULL COMMENT '属性包名',
  `is_pk` tinyint NULL DEFAULT NULL COMMENT '是否主键 0：否  1：是',
  `is_required` tinyint NULL DEFAULT NULL COMMENT '是否必填 0：否  1：是',
  `is_form` tinyint NULL DEFAULT NULL COMMENT '是否表单字段 0：否  1：是',
  `is_list` tinyint NULL DEFAULT NULL COMMENT '是否列表字段 0：否  1：是',
  `is_query` tinyint NULL DEFAULT NULL COMMENT '是否查询字段 0：否  1：是',
  `query_type` varchar(200) NULL DEFAULT NULL COMMENT '查询方式',
  `form_type` varchar(200) NULL DEFAULT NULL COMMENT '表单类型',
  `dict_name` varchar(200) NULL DEFAULT NULL COMMENT '字典名称',
  `validator_type` varchar(200) NULL DEFAULT NULL COMMENT '效验方式',
  `sort` int NULL DEFAULT NULL COMMENT '排序',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `table_name`(`table_name` ASC) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='代码生成表列';

DROP TABLE IF EXISTS `gen_table_info`;
CREATE TABLE `gen_table_info` (
  `id` bigint NOT NULL COMMENT 'id',
  `table_name` varchar(128) NULL DEFAULT NULL COMMENT '表名',
  `class_name` varchar(200) NULL DEFAULT NULL COMMENT '类名',
  `table_comment` varchar(200) NULL DEFAULT NULL COMMENT '功能名',
  `author` varchar(200) NULL DEFAULT NULL COMMENT '作者',
  `email` varchar(200) NULL DEFAULT NULL COMMENT '邮箱',
  `package_name` varchar(200) NULL DEFAULT NULL COMMENT '项目包名',
  `version` varchar(200) NULL DEFAULT NULL COMMENT '项目版本号',
  `backend_path` varchar(500) NULL DEFAULT NULL COMMENT '后端生成路径',
  `frontend_path` varchar(500) NULL DEFAULT NULL COMMENT '前端生成路径',
  `module_name` varchar(200) NULL DEFAULT NULL COMMENT '模块名',
  `sub_module_name` varchar(200) NULL DEFAULT NULL COMMENT '子模块名',
  `datasource_id` bigint NULL DEFAULT NULL COMMENT '数据源ID',
  `baseclass_id` bigint NULL DEFAULT NULL COMMENT '基类ID',
  `created_at` datetime NULL DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `table_name`(`table_name` ASC) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='代码生成表';

DROP TABLE IF EXISTS `gen_template`;
CREATE TABLE `gen_template` (
  `id` bigint NOT NULL COMMENT 'id',
  `name` varchar(200) NULL DEFAULT NULL COMMENT '名称',
  `file_name` varchar(200) NULL DEFAULT NULL COMMENT '文件名',
  `content` mediumtext NOT NULL COMMENT '内容',
  `path` varchar(500) NULL DEFAULT NULL COMMENT '生成路径',
  `status` tinyint NULL DEFAULT NULL COMMENT '状态: 1正常 2暂停 3停用',
  `created_at` datetime NULL DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='模板管理';

SET FOREIGN_KEY_CHECKS = 1;
