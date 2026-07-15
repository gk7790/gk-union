-- gk-union MySQL schema generated from the provided MySQL dump.
-- Source: pasted-text.txt. Navicat metadata is intentionally omitted.

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `sys_dept`;
CREATE TABLE `sys_dept` (
  `id` bigint NOT NULL COMMENT 'id',
  `tenant_id` bigint NULL DEFAULT NULL COMMENT '租户id',
  `pid` bigint NULL DEFAULT 0 COMMENT '上级ID',
  `pids` varchar(500) NULL DEFAULT '' COMMENT '所有上级ID，用逗号分开',
  `name` varchar(50) NULL DEFAULT NULL COMMENT '部门名称',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态',
  `sort` int UNSIGNED NULL DEFAULT 0 COMMENT '排序',
  `remark` varchar(255) NULL DEFAULT NULL COMMENT '备注',
  `created_by` bigint NULL DEFAULT NULL COMMENT '创建者',
  `created_at` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `updated_by` bigint NULL DEFAULT NULL COMMENT '更新者',
  `updated_at` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_pid`(`pid` ASC) USING BTREE,
  INDEX `idx_sort`(`sort` ASC) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='部门管理';

DROP TABLE IF EXISTS `sys_dict_data`;
CREATE TABLE `sys_dict_data` (
  `id` bigint NOT NULL COMMENT 'id',
  `tenant_id` bigint NULL DEFAULT NULL COMMENT '租户id',
  `dict_type_id` bigint NOT NULL COMMENT '字典类型ID',
  `dict_label` varchar(255) NOT NULL COMMENT '字典标签',
  `i18n_key` varchar(255) NULL DEFAULT NULL COMMENT '国际化',
  `dict_value` varchar(128) NULL DEFAULT NULL COMMENT '字典值',
  `attr_type` varchar(255) NULL DEFAULT NULL COMMENT '字典属性',
  `remark` varchar(255) NULL DEFAULT NULL COMMENT '备注',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态',
  `sort` int UNSIGNED NULL DEFAULT NULL COMMENT '排序',
  `color` varchar(255) NULL DEFAULT NULL COMMENT 'UI颜色（可选）',
  `icon` varchar(255) NULL DEFAULT NULL COMMENT '图标（可选）',
  `ext` json NULL COMMENT '扩展字段（重点）',
  `is_default` tinyint NULL DEFAULT NULL COMMENT '是否默认',
  `created_by` bigint NULL DEFAULT NULL COMMENT '创建者',
  `created_at` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `updated_by` bigint NULL DEFAULT NULL COMMENT '更新者',
  `updated_at` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_dict_type_value`(`dict_type_id` ASC, `dict_value` ASC) USING BTREE,
  INDEX `idx_sort`(`sort` ASC) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='字典数据';

DROP TABLE IF EXISTS `sys_dict_type`;
CREATE TABLE `sys_dict_type` (
  `id` bigint NOT NULL COMMENT 'id',
  `tenant_id` bigint NULL DEFAULT NULL COMMENT '租户id',
  `dict_type` varchar(100) NOT NULL COMMENT '字典类型',
  `dict_name` varchar(255) NOT NULL COMMENT '字典名称',
  `remark` varchar(255) NULL DEFAULT NULL COMMENT '备注',
  `sort` int UNSIGNED NULL DEFAULT 0 COMMENT '排序',
  `scope` varchar(255) NULL DEFAULT NULL COMMENT 'PLATFORM / TENANT / USER',
  `created_by` bigint NULL DEFAULT NULL COMMENT '创建者',
  `created_at` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `updated_by` bigint NULL DEFAULT NULL COMMENT '更新者',
  `updated_at` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `dict_type`(`dict_type` ASC) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='字典类型';

DROP TABLE IF EXISTS `sys_i18n`;
CREATE TABLE `sys_i18n` (
  `id` bigint NOT NULL,
  `tenant_id` bigint NULL DEFAULT 0 COMMENT 'NULL=全局；有值=租户覆盖',
  `biz_type` varchar(64) NOT NULL COMMENT 'MENU / DICT / GAME / CONFIG / NOTICE / PAGE',
  `biz_id` bigint NULL DEFAULT NULL COMMENT '对应业务ID',
  `i18n_key` varchar(128) NOT NULL COMMENT '国际化key',
  `lang` varchar(16) NOT NULL COMMENT 'zh_CN / en_US',
  `value` varchar(255) NULL DEFAULT NULL COMMENT '翻译内容',
  `created_by` bigint NOT NULL COMMENT '创建者',
  `created_at` datetime NOT NULL COMMENT '创建时间',
  `updated_by` bigint NOT NULL COMMENT '修改者',
  `updated_at` datetime NOT NULL COMMENT '修改时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统-国际化';

DROP TABLE IF EXISTS `sys_language`;
CREATE TABLE `sys_language` (
  `table_id` bigint NOT NULL COMMENT '表主键',
  `table_name` varchar(32) NOT NULL COMMENT '表名',
  `field_name` varchar(32) NOT NULL COMMENT '字段名',
  `field_value` varchar(200) NOT NULL COMMENT '字段值',
  `language` varchar(10) NOT NULL COMMENT '语言',
  PRIMARY KEY (`table_id`, `table_name`, `field_name`, `language`) USING BTREE,
  INDEX `idx_table_id`(`table_id` ASC) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='国际化';

DROP TABLE IF EXISTS `sys_log_error`;
CREATE TABLE `sys_log_error` (
  `id` bigint NOT NULL COMMENT '主键',
  `trace_id` varchar(64) NULL DEFAULT NULL COMMENT '链路追踪ID',
  `tenant_id` bigint NULL DEFAULT NULL COMMENT '租户ID',
  `user_id` bigint NULL DEFAULT NULL COMMENT '用户ID',
  `username` varchar(64) NULL DEFAULT NULL COMMENT '用户名',
  `module` varchar(64) NULL DEFAULT NULL COMMENT '模块',
  `service_name` varchar(64) NULL DEFAULT NULL COMMENT '服务名',
  `error_type` varchar(64) NULL DEFAULT NULL COMMENT '异常类型',
  `error_code` varchar(64) NULL DEFAULT NULL COMMENT '业务错误码',
  `error_message` text NULL COMMENT '异常消息',
  `stack_trace` longtext NULL COMMENT '完整堆栈',
  `stack_hash` varchar(64) NULL DEFAULT NULL COMMENT '堆栈HASH(用于聚合)',
  `request_uri` varchar(500) NULL DEFAULT NULL COMMENT '请求地址',
  `request_method` varchar(16) NULL DEFAULT NULL COMMENT '请求方式',
  `request_params` longtext NULL COMMENT '请求参数',
  `request_body` longtext NULL COMMENT '请求Body',
  `request_headers` longtext NULL COMMENT '请求头',
  `ip` varchar(64) NULL DEFAULT NULL COMMENT 'IP地址',
  `country` varchar(64) NULL DEFAULT NULL COMMENT '国家',
  `province` varchar(64) NULL DEFAULT NULL COMMENT '省份',
  `city` varchar(64) NULL DEFAULT NULL COMMENT '城市',
  `user_agent` varchar(1000) NULL DEFAULT NULL COMMENT 'UA',
  `device_type` varchar(32) NULL DEFAULT NULL COMMENT '设备类型',
  `os` varchar(64) NULL DEFAULT NULL COMMENT '操作系统',
  `browser` varchar(64) NULL DEFAULT NULL COMMENT '浏览器',
  `http_status` int NULL DEFAULT NULL COMMENT 'HTTP状态码',
  `level` varchar(16) NULL DEFAULT NULL COMMENT '日志级别',
  `resolved` tinyint NULL DEFAULT 0 COMMENT '是否已处理',
  `alarmed` tinyint NULL DEFAULT 0 COMMENT '是否已告警',
  `env` varchar(32) NULL DEFAULT NULL COMMENT '运行环境',
  `created_at` datetime NULL DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_trace_id`(`trace_id` ASC) USING BTREE,
  INDEX `idx_tenant_id`(`tenant_id` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_error_type`(`error_type` ASC) USING BTREE,
  INDEX `idx_stack_hash`(`stack_hash` ASC) USING BTREE,
  INDEX `idx_created_at`(`created_at` ASC) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统异常日志';

DROP TABLE IF EXISTS `sys_log_login`;
CREATE TABLE `sys_log_login` (
  `id` bigint NOT NULL COMMENT 'id',
  `operation` tinyint UNSIGNED NULL DEFAULT NULL COMMENT '用户操作   0：用户登录   1：用户退出',
  `status` tinyint UNSIGNED NOT NULL COMMENT '状态  0：失败    1：成功    2：账号已锁定',
  `user_agent` varchar(500) NULL DEFAULT NULL COMMENT '用户代理',
  `ip` varchar(32) NULL DEFAULT NULL COMMENT '操作IP',
  `created_by_name` varchar(50) NULL DEFAULT NULL COMMENT '用户名',
  `created_by` bigint NULL DEFAULT NULL COMMENT '创建者',
  `created_at` datetime NULL DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_created_at`(`created_at` ASC) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='登录日志';

DROP TABLE IF EXISTS `sys_log_operation`;
CREATE TABLE `sys_log_operation` (
  `id` bigint NOT NULL COMMENT 'id',
  `operation` varchar(50) NULL DEFAULT NULL COMMENT '用户操作',
  `request_uri` varchar(200) NULL DEFAULT NULL COMMENT '请求URI',
  `request_method` varchar(20) NULL DEFAULT NULL COMMENT '请求方式',
  `request_params` mediumtext NULL COMMENT '请求参数',
  `request_time` int UNSIGNED NOT NULL COMMENT '请求时长(毫秒)',
  `user_agent` varchar(500) NULL DEFAULT NULL COMMENT '用户代理',
  `ip` varchar(32) NULL DEFAULT NULL COMMENT '操作IP',
  `status` tinyint UNSIGNED NOT NULL COMMENT '状态  0：失败   1：成功',
  `created_by_name` varchar(50) NULL DEFAULT NULL COMMENT '用户名',
  `created_by` bigint NULL DEFAULT NULL COMMENT '创建者',
  `created_at` datetime NULL DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_created_at`(`created_at` ASC) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志';

DROP TABLE IF EXISTS `sys_menu`;
CREATE TABLE `sys_menu` (
  `id` bigint NOT NULL COMMENT 'id',
  `pid` bigint NULL DEFAULT 0 COMMENT '上级ID，一级菜单为0',
  `name` varchar(255) NULL DEFAULT NULL COMMENT '名称',
  `path` varchar(200) NULL DEFAULT NULL COMMENT '菜单URL',
  `type` varchar(255) NULL DEFAULT NULL COMMENT '类型',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态',
  `auth_code` varchar(500) NULL DEFAULT NULL COMMENT '授权(多个用逗号分隔，如：sys:user:list,sys:user:save)',
  `active_path` varchar(255) NULL DEFAULT NULL COMMENT '激活路径',
  `component` varchar(255) NULL DEFAULT NULL COMMENT '前端组件',
  `meta` json NULL COMMENT '菜单meta',
  `sort` int NULL DEFAULT 0 COMMENT '排序',
  `redirect` varchar(255) NULL DEFAULT NULL COMMENT '重定向',
  `subject_types` json NULL COMMENT '可见主体: PLATFORM/TENANT/MERCHANT',
  `domain` json NULL COMMENT '业务领域:GAMING / CLOAK',
  `created_by` bigint NULL DEFAULT NULL COMMENT '创建者',
  `created_at` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `updated_by` bigint NULL DEFAULT NULL COMMENT '更新者',
  `updated_at` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `unq_name`(`name` ASC) USING BTREE,
  INDEX `idx_pid`(`pid` ASC) USING BTREE,
  INDEX `idx_sort`(`sort` ASC) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='菜单管理';

DROP TABLE IF EXISTS `sys_params`;
CREATE TABLE `sys_params` (
  `id` bigint NOT NULL COMMENT 'id',
  `param_code` varchar(32) NULL DEFAULT NULL COMMENT '参数编码',
  `param_value` varchar(2000) NULL DEFAULT NULL COMMENT '参数值',
  `param_type` tinyint UNSIGNED NULL DEFAULT 1 COMMENT '类型   0：系统参数   1：非系统参数',
  `remark` varchar(200) NULL DEFAULT NULL COMMENT '备注',
  `created_by` bigint NULL DEFAULT NULL COMMENT '创建者',
  `created_at` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `updated_by` bigint NULL DEFAULT NULL COMMENT '更新者',
  `updated_at` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_param_code`(`param_code` ASC) USING BTREE,
  INDEX `idx_created_at`(`created_at` ASC) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='参数管理';

DROP TABLE IF EXISTS `sys_region`;
CREATE TABLE `sys_region` (
  `id` bigint NOT NULL COMMENT 'id',
  `pid` bigint NULL DEFAULT NULL COMMENT '上级ID，一级为0',
  `name` varchar(100) NULL DEFAULT NULL COMMENT '名称',
  `tree_level` tinyint NULL DEFAULT NULL COMMENT '层级',
  `leaf` tinyint NULL DEFAULT NULL COMMENT '是否叶子节点  0：否   1：是',
  `sort` bigint NULL DEFAULT NULL COMMENT '排序',
  `created_by` bigint NULL DEFAULT NULL COMMENT '创建者',
  `created_at` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `updated_by` bigint NULL DEFAULT NULL COMMENT '更新者',
  `updated_at` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='行政区域';

DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role` (
  `id` bigint NOT NULL COMMENT 'id',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户ID；0=系统模板，1=平台专属，≥2=租户实例',
  `dept_id` bigint NOT NULL DEFAULT 0 COMMENT '部门ID；0=不限制部门',
  `name` varchar(50) NULL DEFAULT NULL COMMENT '角色名称',
  `auth` varchar(255) NULL DEFAULT NULL COMMENT '角色标识',
  `data_scope` tinyint NULL DEFAULT NULL COMMENT '数据范围: ALL/TENANT_ALL/SELF_AND_CHILDREN/SELF',
  `role_scope` varchar(255) NULL DEFAULT NULL COMMENT '角色作用域: PLATFORM/TENANT/MERCHANT',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态',
  `remark` varchar(100) NULL DEFAULT NULL COMMENT '备注',
  `created_by` bigint NULL DEFAULT NULL COMMENT '创建者',
  `created_at` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `updated_by` bigint NULL DEFAULT NULL COMMENT '更新者',
  `updated_at` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `unq_auth`(`tenant_id` ASC, `auth` ASC) USING BTREE,
  INDEX `idx_dept_id`(`dept_id` ASC) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色管理';

DROP TABLE IF EXISTS `sys_role_data_scope`;
CREATE TABLE `sys_role_data_scope` (
  `id` bigint NOT NULL COMMENT 'id',
  `role_id` bigint NULL DEFAULT NULL COMMENT '角色ID',
  `dept_id` bigint NULL DEFAULT NULL COMMENT '部门ID',
  `created_by` bigint NULL DEFAULT NULL COMMENT '创建者',
  `created_at` datetime NULL DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_role_id`(`role_id` ASC) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色数据权限';

DROP TABLE IF EXISTS `sys_role_menu`;
CREATE TABLE `sys_role_menu` (
  `id` bigint NOT NULL COMMENT 'id',
  `role_id` bigint NULL DEFAULT NULL COMMENT '角色ID',
  `menu_id` bigint NULL DEFAULT NULL COMMENT '菜单ID',
  `created_by` bigint NULL DEFAULT NULL COMMENT '创建者',
  `created_at` datetime NULL DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_role_id`(`role_id` ASC) USING BTREE,
  INDEX `idx_menu_id`(`menu_id` ASC) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色菜单关系';

DROP TABLE IF EXISTS `sys_role_user`;
CREATE TABLE `sys_role_user` (
  `id` bigint NOT NULL COMMENT 'id',
  `user_subject_id` bigint NULL DEFAULT NULL COMMENT '用户主体ID，关联 sys_user_subject.id',
  `role_id` bigint NULL DEFAULT NULL COMMENT '角色ID',
  `user_id` bigint NULL DEFAULT NULL COMMENT '用户ID',
  `created_by` bigint NULL DEFAULT NULL COMMENT '创建者',
  `created_at` datetime NULL DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_role_id`(`role_id` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色用户关系';

DROP TABLE IF EXISTS `sys_tenant`;
CREATE TABLE `sys_tenant` (
  `id` bigint NULL DEFAULT NULL,
  `name` varchar(255) NULL DEFAULT NULL COMMENT '租户公司名称',
  `code` varchar(100) NULL DEFAULT NULL COMMENT '编码',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态',
  `domain` varchar(255) NULL DEFAULT NULL COMMENT '域名',
  `currency` varchar(20) NULL DEFAULT NULL COMMENT '货币',
  `timezone` varchar(50) NULL DEFAULT 'UTC' COMMENT '时区',
  `lang` varchar(255) NULL DEFAULT 'en-US' COMMENT '语言',
  `api_key` varchar(255) NULL DEFAULT NULL COMMENT '租户key',
  `api_secret` varchar(255) NULL DEFAULT NULL COMMENT '私密密钥',
  `remark` varchar(255) NULL DEFAULT NULL COMMENT '备注',
  `created_by` bigint NULL DEFAULT NULL COMMENT '创建者',
  `created_at` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `updated_by` bigint NULL DEFAULT NULL COMMENT '修改者',
  `updated_at` datetime NULL DEFAULT NULL COMMENT '修改时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='租户信息表';

DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
  `id` bigint NOT NULL COMMENT 'id',
  `nickname` varchar(255) NULL DEFAULT NULL COMMENT '昵称',
  `username` varchar(50) NOT NULL COMMENT '用户名',
  `password` varchar(100) NOT NULL COMMENT '密码哈希',
  `real_name` varchar(50) NULL DEFAULT NULL COMMENT '姓名',
  `avatar` varchar(200) NULL DEFAULT NULL COMMENT '头像',
  `gender` tinyint UNSIGNED NULL DEFAULT NULL COMMENT '性别   0：男   1：女    2：保密',
  `email` varchar(100) NULL DEFAULT NULL COMMENT '邮箱',
  `mobile` varchar(100) NULL DEFAULT NULL COMMENT '手机号',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态  0：停用   1：正常',
  `remark` varchar(255) NULL DEFAULT NULL COMMENT '备注',
  `created_by` bigint NULL DEFAULT NULL COMMENT '创建者',
  `created_at` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `updated_by` bigint NULL DEFAULT NULL COMMENT '更新者',
  `updated_at` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `auth_type` tinyint NULL DEFAULT 1 COMMENT '验证器类型',
  `auth_secret` varchar(255) NULL DEFAULT NULL COMMENT '验证器秘钥',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_username`(`username` ASC) USING BTREE,
  UNIQUE INDEX `uk_sys_user_mobile`(`mobile` ASC) USING BTREE,
  UNIQUE INDEX `uk_sys_user_email`(`email` ASC) USING BTREE,
  INDEX `idx_created_at`(`created_at` ASC) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统用户';

DROP TABLE IF EXISTS `sys_user_subject`;
CREATE TABLE `sys_user_subject` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID，关联sys_user.id',
  `subject_type` varchar(32) NOT NULL COMMENT '主体类型: PLATFORM/TENANT/MERCHANT',
  `tenant_id` bigint NULL DEFAULT NULL COMMENT '租户ID；TENANT/MERCHANT主体必填',
  `subject_id` bigint NULL DEFAULT NULL COMMENT '主体ID；TENANT主体为空，MERCHANT主体为商户ID',
  `dept_id` bigint NULL DEFAULT NULL COMMENT '租户内部门ID；TENANT主体可填',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态: 0禁用 1启用',
  `remark` varchar(512) NULL DEFAULT NULL COMMENT '备注',
  `created_by` bigint NULL DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_by` bigint NULL DEFAULT NULL COMMENT '更新人ID',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_sys_user_subject_user`(`user_id` ASC) USING BTREE,
  INDEX `idx_sys_user_subject_type`(`subject_type` ASC, `status` ASC) USING BTREE,
  INDEX `idx_sys_user_subject_tenant`(`tenant_id` ASC, `status` ASC) USING BTREE,
  INDEX `idx_sys_user_subject_subject`(`tenant_id` ASC, `subject_id` ASC, `status` ASC) USING BTREE,
  CONSTRAINT `chk_sys_user_subject_status` CHECK (`status` in (0,1)),
  CONSTRAINT `chk_sys_user_subject_tenant` CHECK ((`subject_type` <> 'TENANT') or ((`tenant_id` is not null) and (`subject_id` is null))),
  CONSTRAINT `chk_sys_user_subject_type` CHECK (`subject_type` in ('PLATFORM','TENANT','MERCHANT'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户唯一主体身份表';

SET FOREIGN_KEY_CHECKS = 1;
