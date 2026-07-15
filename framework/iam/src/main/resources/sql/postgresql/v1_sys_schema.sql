-- gk-union PostgreSQL schema generated from the provided MySQL dump.
-- Source: pasted-text.txt. Navicat metadata is intentionally omitted.

DROP TABLE IF EXISTS sys_dept CASCADE;
CREATE TABLE sys_dept (
  id bigint NOT NULL,
  tenant_id bigint NULL,
  pid bigint NULL DEFAULT 0,
  pids varchar(500) NULL DEFAULT '',
  name varchar(50) NULL,
  status smallint NULL DEFAULT 1,
  sort integer NULL DEFAULT 0,
  remark varchar(255) NULL,
  created_by bigint NULL,
  created_at timestamp NULL,
  updated_by bigint NULL,
  updated_at timestamp NULL,
  CONSTRAINT pk_sys_dept PRIMARY KEY (id)
);
COMMENT ON TABLE sys_dept IS '部门管理';
COMMENT ON COLUMN sys_dept.id IS 'id';
COMMENT ON COLUMN sys_dept.tenant_id IS '租户id';
COMMENT ON COLUMN sys_dept.pid IS '上级ID';
COMMENT ON COLUMN sys_dept.pids IS '所有上级ID，用逗号分开';
COMMENT ON COLUMN sys_dept.name IS '部门名称';
COMMENT ON COLUMN sys_dept.status IS '状态';
COMMENT ON COLUMN sys_dept.sort IS '排序';
COMMENT ON COLUMN sys_dept.remark IS '备注';
COMMENT ON COLUMN sys_dept.created_by IS '创建者';
COMMENT ON COLUMN sys_dept.created_at IS '创建时间';
COMMENT ON COLUMN sys_dept.updated_by IS '更新者';
COMMENT ON COLUMN sys_dept.updated_at IS '更新时间';
CREATE INDEX idx_sys_dept_idx_pid ON sys_dept (pid);
CREATE INDEX idx_sys_dept_idx_sort ON sys_dept (sort);

DROP TABLE IF EXISTS sys_dict_data CASCADE;
CREATE TABLE sys_dict_data (
  id bigint NOT NULL,
  tenant_id bigint NULL,
  dict_type_id bigint NOT NULL,
  dict_label varchar(255) NOT NULL,
  i18n_key varchar(255) NULL,
  dict_value varchar(128) NULL,
  attr_type varchar(255) NULL,
  remark varchar(255) NULL,
  status smallint NULL DEFAULT 1,
  sort integer NULL,
  color varchar(255) NULL,
  icon varchar(255) NULL,
  ext json NULL,
  is_default smallint NULL,
  created_by bigint NULL,
  created_at timestamp NULL,
  updated_by bigint NULL,
  updated_at timestamp NULL,
  CONSTRAINT pk_sys_dict_data PRIMARY KEY (id)
);
COMMENT ON TABLE sys_dict_data IS '字典数据';
COMMENT ON COLUMN sys_dict_data.id IS 'id';
COMMENT ON COLUMN sys_dict_data.tenant_id IS '租户id';
COMMENT ON COLUMN sys_dict_data.dict_type_id IS '字典类型ID';
COMMENT ON COLUMN sys_dict_data.dict_label IS '字典标签';
COMMENT ON COLUMN sys_dict_data.i18n_key IS '国际化';
COMMENT ON COLUMN sys_dict_data.dict_value IS '字典值';
COMMENT ON COLUMN sys_dict_data.attr_type IS '字典属性';
COMMENT ON COLUMN sys_dict_data.remark IS '备注';
COMMENT ON COLUMN sys_dict_data.status IS '状态';
COMMENT ON COLUMN sys_dict_data.sort IS '排序';
COMMENT ON COLUMN sys_dict_data.color IS 'UI颜色（可选）';
COMMENT ON COLUMN sys_dict_data.icon IS '图标（可选）';
COMMENT ON COLUMN sys_dict_data.ext IS '扩展字段（重点）';
COMMENT ON COLUMN sys_dict_data.is_default IS '是否默认';
COMMENT ON COLUMN sys_dict_data.created_by IS '创建者';
COMMENT ON COLUMN sys_dict_data.created_at IS '创建时间';
COMMENT ON COLUMN sys_dict_data.updated_by IS '更新者';
COMMENT ON COLUMN sys_dict_data.updated_at IS '更新时间';
CREATE UNIQUE INDEX uk_sys_dict_data_uk_dict_type_value ON sys_dict_data (dict_type_id, dict_value);
CREATE INDEX idx_sys_dict_data_idx_sort ON sys_dict_data (sort);

DROP TABLE IF EXISTS sys_dict_type CASCADE;
CREATE TABLE sys_dict_type (
  id bigint NOT NULL,
  tenant_id bigint NULL,
  dict_type varchar(100) NOT NULL,
  dict_name varchar(255) NOT NULL,
  remark varchar(255) NULL,
  sort integer NULL DEFAULT 0,
  scope varchar(255) NULL,
  created_by bigint NULL,
  created_at timestamp NULL,
  updated_by bigint NULL,
  updated_at timestamp NULL,
  CONSTRAINT pk_sys_dict_type PRIMARY KEY (id)
);
COMMENT ON TABLE sys_dict_type IS '字典类型';
COMMENT ON COLUMN sys_dict_type.id IS 'id';
COMMENT ON COLUMN sys_dict_type.tenant_id IS '租户id';
COMMENT ON COLUMN sys_dict_type.dict_type IS '字典类型';
COMMENT ON COLUMN sys_dict_type.dict_name IS '字典名称';
COMMENT ON COLUMN sys_dict_type.remark IS '备注';
COMMENT ON COLUMN sys_dict_type.sort IS '排序';
COMMENT ON COLUMN sys_dict_type.scope IS 'PLATFORM / TENANT / USER';
COMMENT ON COLUMN sys_dict_type.created_by IS '创建者';
COMMENT ON COLUMN sys_dict_type.created_at IS '创建时间';
COMMENT ON COLUMN sys_dict_type.updated_by IS '更新者';
COMMENT ON COLUMN sys_dict_type.updated_at IS '更新时间';
CREATE UNIQUE INDEX uk_sys_dict_type_dict_type ON sys_dict_type (dict_type);

DROP TABLE IF EXISTS sys_i18n CASCADE;
CREATE TABLE sys_i18n (
  id bigint NOT NULL,
  tenant_id bigint NULL DEFAULT 0,
  biz_type varchar(64) NOT NULL,
  biz_id bigint NULL,
  i18n_key varchar(128) NOT NULL,
  lang varchar(16) NOT NULL,
  value varchar(255) NULL,
  created_by bigint NOT NULL,
  created_at timestamp NOT NULL,
  updated_by bigint NOT NULL,
  updated_at timestamp NOT NULL,
  CONSTRAINT pk_sys_i18n PRIMARY KEY (id)
);
COMMENT ON TABLE sys_i18n IS '系统-国际化';
COMMENT ON COLUMN sys_i18n.tenant_id IS 'NULL=全局；有值=租户覆盖';
COMMENT ON COLUMN sys_i18n.biz_type IS 'MENU / DICT / GAME / CONFIG / NOTICE / PAGE';
COMMENT ON COLUMN sys_i18n.biz_id IS '对应业务ID';
COMMENT ON COLUMN sys_i18n.i18n_key IS '国际化key';
COMMENT ON COLUMN sys_i18n.lang IS 'zh_CN / en_US';
COMMENT ON COLUMN sys_i18n.value IS '翻译内容';
COMMENT ON COLUMN sys_i18n.created_by IS '创建者';
COMMENT ON COLUMN sys_i18n.created_at IS '创建时间';
COMMENT ON COLUMN sys_i18n.updated_by IS '修改者';
COMMENT ON COLUMN sys_i18n.updated_at IS '修改时间';

DROP TABLE IF EXISTS sys_log_error CASCADE;
CREATE TABLE sys_log_error (
  id bigint NOT NULL,
  trace_id varchar(64) NULL,
  tenant_id bigint NULL,
  user_id bigint NULL,
  username varchar(64) NULL,
  module varchar(64) NULL,
  service_name varchar(64) NULL,
  error_type varchar(64) NULL,
  error_code varchar(64) NULL,
  error_message text NULL,
  stack_trace text NULL,
  stack_hash varchar(64) NULL,
  request_uri varchar(500) NULL,
  request_method varchar(16) NULL,
  request_params text NULL,
  request_body text NULL,
  request_headers text NULL,
  ip varchar(64) NULL,
  country varchar(64) NULL,
  province varchar(64) NULL,
  city varchar(64) NULL,
  user_agent varchar(1000) NULL,
  device_type varchar(32) NULL,
  os varchar(64) NULL,
  browser varchar(64) NULL,
  http_status integer NULL,
  level varchar(16) NULL,
  resolved smallint NULL DEFAULT 0,
  alarmed smallint NULL DEFAULT 0,
  env varchar(32) NULL,
  created_at timestamp NULL,
  CONSTRAINT pk_sys_log_error PRIMARY KEY (id)
);
COMMENT ON TABLE sys_log_error IS '系统异常日志';
COMMENT ON COLUMN sys_log_error.id IS '主键';
COMMENT ON COLUMN sys_log_error.trace_id IS '链路追踪ID';
COMMENT ON COLUMN sys_log_error.tenant_id IS '租户ID';
COMMENT ON COLUMN sys_log_error.user_id IS '用户ID';
COMMENT ON COLUMN sys_log_error.username IS '用户名';
COMMENT ON COLUMN sys_log_error.module IS '模块';
COMMENT ON COLUMN sys_log_error.service_name IS '服务名';
COMMENT ON COLUMN sys_log_error.error_type IS '异常类型';
COMMENT ON COLUMN sys_log_error.error_code IS '业务错误码';
COMMENT ON COLUMN sys_log_error.error_message IS '异常消息';
COMMENT ON COLUMN sys_log_error.stack_trace IS '完整堆栈';
COMMENT ON COLUMN sys_log_error.stack_hash IS '堆栈HASH(用于聚合)';
COMMENT ON COLUMN sys_log_error.request_uri IS '请求地址';
COMMENT ON COLUMN sys_log_error.request_method IS '请求方式';
COMMENT ON COLUMN sys_log_error.request_params IS '请求参数';
COMMENT ON COLUMN sys_log_error.request_body IS '请求Body';
COMMENT ON COLUMN sys_log_error.request_headers IS '请求头';
COMMENT ON COLUMN sys_log_error.ip IS 'IP地址';
COMMENT ON COLUMN sys_log_error.country IS '国家';
COMMENT ON COLUMN sys_log_error.province IS '省份';
COMMENT ON COLUMN sys_log_error.city IS '城市';
COMMENT ON COLUMN sys_log_error.user_agent IS 'UA';
COMMENT ON COLUMN sys_log_error.device_type IS '设备类型';
COMMENT ON COLUMN sys_log_error.os IS '操作系统';
COMMENT ON COLUMN sys_log_error.browser IS '浏览器';
COMMENT ON COLUMN sys_log_error.http_status IS 'HTTP状态码';
COMMENT ON COLUMN sys_log_error.level IS '日志级别';
COMMENT ON COLUMN sys_log_error.resolved IS '是否已处理';
COMMENT ON COLUMN sys_log_error.alarmed IS '是否已告警';
COMMENT ON COLUMN sys_log_error.env IS '运行环境';
COMMENT ON COLUMN sys_log_error.created_at IS '创建时间';
CREATE INDEX idx_sys_log_error_idx_trace_id ON sys_log_error (trace_id);
CREATE INDEX idx_sys_log_error_idx_tenant_id ON sys_log_error (tenant_id);
CREATE INDEX idx_sys_log_error_idx_user_id ON sys_log_error (user_id);
CREATE INDEX idx_sys_log_error_idx_error_type ON sys_log_error (error_type);
CREATE INDEX idx_sys_log_error_idx_stack_hash ON sys_log_error (stack_hash);
CREATE INDEX idx_sys_log_error_idx_created_at ON sys_log_error (created_at);

DROP TABLE IF EXISTS sys_log_login CASCADE;
CREATE TABLE sys_log_login (
  id bigint NOT NULL,
  operation smallint NULL,
  status smallint NOT NULL,
  user_agent varchar(500) NULL,
  ip varchar(32) NULL,
  created_by_name varchar(50) NULL,
  created_by bigint NULL,
  created_at timestamp NULL,
  CONSTRAINT pk_sys_log_login PRIMARY KEY (id)
);
COMMENT ON TABLE sys_log_login IS '登录日志';
COMMENT ON COLUMN sys_log_login.id IS 'id';
COMMENT ON COLUMN sys_log_login.operation IS '用户操作   0：用户登录   1：用户退出';
COMMENT ON COLUMN sys_log_login.status IS '状态  0：失败    1：成功    2：账号已锁定';
COMMENT ON COLUMN sys_log_login.user_agent IS '用户代理';
COMMENT ON COLUMN sys_log_login.ip IS '操作IP';
COMMENT ON COLUMN sys_log_login.created_by_name IS '用户名';
COMMENT ON COLUMN sys_log_login.created_by IS '创建者';
COMMENT ON COLUMN sys_log_login.created_at IS '创建时间';
CREATE INDEX idx_sys_log_login_idx_status ON sys_log_login (status);
CREATE INDEX idx_sys_log_login_idx_created_at ON sys_log_login (created_at);

DROP TABLE IF EXISTS sys_log_operation CASCADE;
CREATE TABLE sys_log_operation (
  id bigint NOT NULL,
  operation varchar(50) NULL,
  request_uri varchar(200) NULL,
  request_method varchar(20) NULL,
  request_params text NULL,
  request_time integer NOT NULL,
  user_agent varchar(500) NULL,
  ip varchar(32) NULL,
  status smallint NOT NULL,
  created_by_name varchar(50) NULL,
  created_by bigint NULL,
  created_at timestamp NULL,
  CONSTRAINT pk_sys_log_operation PRIMARY KEY (id)
);
COMMENT ON TABLE sys_log_operation IS '操作日志';
COMMENT ON COLUMN sys_log_operation.id IS 'id';
COMMENT ON COLUMN sys_log_operation.operation IS '用户操作';
COMMENT ON COLUMN sys_log_operation.request_uri IS '请求URI';
COMMENT ON COLUMN sys_log_operation.request_method IS '请求方式';
COMMENT ON COLUMN sys_log_operation.request_params IS '请求参数';
COMMENT ON COLUMN sys_log_operation.request_time IS '请求时长(毫秒)';
COMMENT ON COLUMN sys_log_operation.user_agent IS '用户代理';
COMMENT ON COLUMN sys_log_operation.ip IS '操作IP';
COMMENT ON COLUMN sys_log_operation.status IS '状态  0：失败   1：成功';
COMMENT ON COLUMN sys_log_operation.created_by_name IS '用户名';
COMMENT ON COLUMN sys_log_operation.created_by IS '创建者';
COMMENT ON COLUMN sys_log_operation.created_at IS '创建时间';
CREATE INDEX idx_sys_log_operation_idx_created_at ON sys_log_operation (created_at);

DROP TABLE IF EXISTS sys_menu CASCADE;
CREATE TABLE sys_menu (
  id bigint NOT NULL,
  pid bigint NULL DEFAULT 0,
  name varchar(255) NULL,
  path varchar(200) NULL,
  type varchar(255) NULL,
  status smallint NULL DEFAULT 1,
  auth_code varchar(500) NULL,
  active_path varchar(255) NULL,
  component varchar(255) NULL,
  meta json NULL,
  sort integer NULL DEFAULT 0,
  redirect varchar(255) NULL,
  subject_types json NULL,
  domain json NULL,
  created_by bigint NULL,
  created_at timestamp NULL,
  updated_by bigint NULL,
  updated_at timestamp NULL,
  CONSTRAINT pk_sys_menu PRIMARY KEY (id)
);
COMMENT ON TABLE sys_menu IS '菜单管理';
COMMENT ON COLUMN sys_menu.id IS 'id';
COMMENT ON COLUMN sys_menu.pid IS '上级ID，一级菜单为0';
COMMENT ON COLUMN sys_menu.name IS '名称';
COMMENT ON COLUMN sys_menu.path IS '菜单URL';
COMMENT ON COLUMN sys_menu.type IS '类型';
COMMENT ON COLUMN sys_menu.status IS '状态';
COMMENT ON COLUMN sys_menu.auth_code IS '授权(多个用逗号分隔，如：sys:user:list,sys:user:save)';
COMMENT ON COLUMN sys_menu.active_path IS '激活路径';
COMMENT ON COLUMN sys_menu.component IS '前端组件';
COMMENT ON COLUMN sys_menu.meta IS '菜单meta';
COMMENT ON COLUMN sys_menu.sort IS '排序';
COMMENT ON COLUMN sys_menu.redirect IS '重定向';
COMMENT ON COLUMN sys_menu.subject_types IS '可见主体: PLATFORM/TENANT/MERCHANT';
COMMENT ON COLUMN sys_menu.domain IS '业务领域:GAMING / CLOAK';
COMMENT ON COLUMN sys_menu.created_by IS '创建者';
COMMENT ON COLUMN sys_menu.created_at IS '创建时间';
COMMENT ON COLUMN sys_menu.updated_by IS '更新者';
COMMENT ON COLUMN sys_menu.updated_at IS '更新时间';
CREATE UNIQUE INDEX uk_sys_menu_unq_name ON sys_menu (name);
CREATE INDEX idx_sys_menu_idx_pid ON sys_menu (pid);
CREATE INDEX idx_sys_menu_idx_sort ON sys_menu (sort);

DROP TABLE IF EXISTS sys_params CASCADE;
CREATE TABLE sys_params (
  id bigint NOT NULL,
  param_code varchar(32) NULL,
  param_value varchar(2000) NULL,
  param_type smallint NULL DEFAULT 1,
  remark varchar(200) NULL,
  created_by bigint NULL,
  created_at timestamp NULL,
  updated_by bigint NULL,
  updated_at timestamp NULL,
  CONSTRAINT pk_sys_params PRIMARY KEY (id)
);
COMMENT ON TABLE sys_params IS '参数管理';
COMMENT ON COLUMN sys_params.id IS 'id';
COMMENT ON COLUMN sys_params.param_code IS '参数编码';
COMMENT ON COLUMN sys_params.param_value IS '参数值';
COMMENT ON COLUMN sys_params.param_type IS '类型   0：系统参数   1：非系统参数';
COMMENT ON COLUMN sys_params.remark IS '备注';
COMMENT ON COLUMN sys_params.created_by IS '创建者';
COMMENT ON COLUMN sys_params.created_at IS '创建时间';
COMMENT ON COLUMN sys_params.updated_by IS '更新者';
COMMENT ON COLUMN sys_params.updated_at IS '更新时间';
CREATE UNIQUE INDEX uk_sys_params_uk_param_code ON sys_params (param_code);
CREATE INDEX idx_sys_params_idx_created_at ON sys_params (created_at);

DROP TABLE IF EXISTS sys_region CASCADE;
CREATE TABLE sys_region (
  id bigint NOT NULL,
  pid bigint NULL,
  name varchar(100) NULL,
  tree_level smallint NULL,
  leaf smallint NULL,
  sort bigint NULL,
  created_by bigint NULL,
  created_at timestamp NULL,
  updated_by bigint NULL,
  updated_at timestamp NULL,
  CONSTRAINT pk_sys_region PRIMARY KEY (id)
);
COMMENT ON TABLE sys_region IS '行政区域';
COMMENT ON COLUMN sys_region.id IS 'id';
COMMENT ON COLUMN sys_region.pid IS '上级ID，一级为0';
COMMENT ON COLUMN sys_region.name IS '名称';
COMMENT ON COLUMN sys_region.tree_level IS '层级';
COMMENT ON COLUMN sys_region.leaf IS '是否叶子节点  0：否   1：是';
COMMENT ON COLUMN sys_region.sort IS '排序';
COMMENT ON COLUMN sys_region.created_by IS '创建者';
COMMENT ON COLUMN sys_region.created_at IS '创建时间';
COMMENT ON COLUMN sys_region.updated_by IS '更新者';
COMMENT ON COLUMN sys_region.updated_at IS '更新时间';

DROP TABLE IF EXISTS sys_role CASCADE;
CREATE TABLE sys_role (
  id bigint NOT NULL,
  tenant_id bigint NOT NULL DEFAULT 0,
  dept_id bigint NOT NULL DEFAULT 0,
  name varchar(50) NULL,
  auth varchar(255) NULL,
  data_scope smallint NULL,
  role_scope varchar(255) NULL,
  status smallint NULL DEFAULT 1,
  remark varchar(100) NULL,
  created_by bigint NULL,
  created_at timestamp NULL,
  updated_by bigint NULL,
  updated_at timestamp NULL,
  CONSTRAINT pk_sys_role PRIMARY KEY (id)
);
COMMENT ON TABLE sys_role IS '角色管理';
COMMENT ON COLUMN sys_role.id IS 'id';
COMMENT ON COLUMN sys_role.tenant_id IS '租户ID；0=系统模板，1=平台专属，≥2=租户实例';
COMMENT ON COLUMN sys_role.dept_id IS '部门ID；0=不限制部门';
COMMENT ON COLUMN sys_role.name IS '角色名称';
COMMENT ON COLUMN sys_role.auth IS '角色标识';
COMMENT ON COLUMN sys_role.data_scope IS '数据范围: ALL/TENANT_ALL/SELF_AND_CHILDREN/SELF';
COMMENT ON COLUMN sys_role.role_scope IS '角色作用域: PLATFORM/TENANT/MERCHANT';
COMMENT ON COLUMN sys_role.status IS '状态';
COMMENT ON COLUMN sys_role.remark IS '备注';
COMMENT ON COLUMN sys_role.created_by IS '创建者';
COMMENT ON COLUMN sys_role.created_at IS '创建时间';
COMMENT ON COLUMN sys_role.updated_by IS '更新者';
COMMENT ON COLUMN sys_role.updated_at IS '更新时间';
CREATE UNIQUE INDEX uk_sys_role_unq_auth ON sys_role (tenant_id, auth);
CREATE INDEX idx_sys_role_idx_dept_id ON sys_role (dept_id);

DROP TABLE IF EXISTS sys_role_data_scope CASCADE;
CREATE TABLE sys_role_data_scope (
  id bigint NOT NULL,
  role_id bigint NULL,
  dept_id bigint NULL,
  created_by bigint NULL,
  created_at timestamp NULL,
  CONSTRAINT pk_sys_role_data_scope PRIMARY KEY (id)
);
COMMENT ON TABLE sys_role_data_scope IS '角色数据权限';
COMMENT ON COLUMN sys_role_data_scope.id IS 'id';
COMMENT ON COLUMN sys_role_data_scope.role_id IS '角色ID';
COMMENT ON COLUMN sys_role_data_scope.dept_id IS '部门ID';
COMMENT ON COLUMN sys_role_data_scope.created_by IS '创建者';
COMMENT ON COLUMN sys_role_data_scope.created_at IS '创建时间';
CREATE INDEX idx_sys_role_data_scope_idx_role_id ON sys_role_data_scope (role_id);

DROP TABLE IF EXISTS sys_role_menu CASCADE;
CREATE TABLE sys_role_menu (
  id bigint NOT NULL,
  role_id bigint NULL,
  menu_id bigint NULL,
  created_by bigint NULL,
  created_at timestamp NULL,
  CONSTRAINT pk_sys_role_menu PRIMARY KEY (id)
);
COMMENT ON TABLE sys_role_menu IS '角色菜单关系';
COMMENT ON COLUMN sys_role_menu.id IS 'id';
COMMENT ON COLUMN sys_role_menu.role_id IS '角色ID';
COMMENT ON COLUMN sys_role_menu.menu_id IS '菜单ID';
COMMENT ON COLUMN sys_role_menu.created_by IS '创建者';
COMMENT ON COLUMN sys_role_menu.created_at IS '创建时间';
CREATE INDEX idx_sys_role_menu_idx_role_id ON sys_role_menu (role_id);
CREATE INDEX idx_sys_role_menu_idx_menu_id ON sys_role_menu (menu_id);

DROP TABLE IF EXISTS sys_role_user CASCADE;
CREATE TABLE sys_role_user (
  id bigint NOT NULL,
  user_subject_id bigint NULL,
  role_id bigint NULL,
  user_id bigint NULL,
  created_by bigint NULL,
  created_at timestamp NULL,
  CONSTRAINT pk_sys_role_user PRIMARY KEY (id)
);
COMMENT ON TABLE sys_role_user IS '角色用户关系';
COMMENT ON COLUMN sys_role_user.id IS 'id';
COMMENT ON COLUMN sys_role_user.user_subject_id IS '用户主体ID，关联 sys_user_subject.id';
COMMENT ON COLUMN sys_role_user.role_id IS '角色ID';
COMMENT ON COLUMN sys_role_user.user_id IS '用户ID';
COMMENT ON COLUMN sys_role_user.created_by IS '创建者';
COMMENT ON COLUMN sys_role_user.created_at IS '创建时间';
CREATE INDEX idx_sys_role_user_idx_role_id ON sys_role_user (role_id);
CREATE INDEX idx_sys_role_user_idx_user_id ON sys_role_user (user_id);

DROP TABLE IF EXISTS sys_tenant CASCADE;
CREATE TABLE sys_tenant (
  id bigint NULL,
  name varchar(255) NULL,
  code varchar(100) NULL,
  status smallint NULL DEFAULT 1,
  domain varchar(255) NULL,
  currency varchar(20) NULL,
  timezone varchar(50) NULL DEFAULT 'UTC',
  lang varchar(255) NULL DEFAULT 'en-US',
  api_key varchar(255) NULL,
  api_secret varchar(255) NULL,
  remark varchar(255) NULL,
  created_by bigint NULL,
  created_at timestamp NULL,
  updated_by bigint NULL,
  updated_at timestamp NULL
);
COMMENT ON TABLE sys_tenant IS '租户信息表';
COMMENT ON COLUMN sys_tenant.name IS '租户公司名称';
COMMENT ON COLUMN sys_tenant.code IS '编码';
COMMENT ON COLUMN sys_tenant.status IS '状态';
COMMENT ON COLUMN sys_tenant.domain IS '域名';
COMMENT ON COLUMN sys_tenant.currency IS '货币';
COMMENT ON COLUMN sys_tenant.timezone IS '时区';
COMMENT ON COLUMN sys_tenant.lang IS '语言';
COMMENT ON COLUMN sys_tenant.api_key IS '租户key';
COMMENT ON COLUMN sys_tenant.api_secret IS '私密密钥';
COMMENT ON COLUMN sys_tenant.remark IS '备注';
COMMENT ON COLUMN sys_tenant.created_by IS '创建者';
COMMENT ON COLUMN sys_tenant.created_at IS '创建时间';
COMMENT ON COLUMN sys_tenant.updated_by IS '修改者';
COMMENT ON COLUMN sys_tenant.updated_at IS '修改时间';

DROP TABLE IF EXISTS sys_user CASCADE;
CREATE TABLE sys_user (
  id bigint NOT NULL,
  nickname varchar(255) NULL,
  username varchar(50) NOT NULL,
  password varchar(100) NOT NULL,
  real_name varchar(50) NULL,
  avatar varchar(200) NULL,
  gender smallint NULL,
  email varchar(100) NULL,
  mobile varchar(100) NULL,
  status smallint NOT NULL DEFAULT 1,
  remark varchar(255) NULL,
  created_by bigint NULL,
  created_at timestamp NULL,
  updated_by bigint NULL,
  updated_at timestamp NULL,
  auth_type smallint NULL DEFAULT 1,
  auth_secret varchar(255) NULL,
  CONSTRAINT pk_sys_user PRIMARY KEY (id)
);
COMMENT ON TABLE sys_user IS '系统用户';
COMMENT ON COLUMN sys_user.id IS 'id';
COMMENT ON COLUMN sys_user.nickname IS '昵称';
COMMENT ON COLUMN sys_user.username IS '用户名';
COMMENT ON COLUMN sys_user.password IS '密码哈希';
COMMENT ON COLUMN sys_user.real_name IS '姓名';
COMMENT ON COLUMN sys_user.avatar IS '头像';
COMMENT ON COLUMN sys_user.gender IS '性别   0：男   1：女    2：保密';
COMMENT ON COLUMN sys_user.email IS '邮箱';
COMMENT ON COLUMN sys_user.mobile IS '手机号';
COMMENT ON COLUMN sys_user.status IS '状态  0：停用   1：正常';
COMMENT ON COLUMN sys_user.remark IS '备注';
COMMENT ON COLUMN sys_user.created_by IS '创建者';
COMMENT ON COLUMN sys_user.created_at IS '创建时间';
COMMENT ON COLUMN sys_user.updated_by IS '更新者';
COMMENT ON COLUMN sys_user.updated_at IS '更新时间';
COMMENT ON COLUMN sys_user.auth_type IS '验证器类型';
COMMENT ON COLUMN sys_user.auth_secret IS '验证器秘钥';
CREATE UNIQUE INDEX uk_sys_user_uk_username ON sys_user (username);
CREATE UNIQUE INDEX uk_sys_user_uk_sys_user_mobile ON sys_user (mobile);
CREATE UNIQUE INDEX uk_sys_user_uk_sys_user_email ON sys_user (email);
CREATE INDEX idx_sys_user_idx_created_at ON sys_user (created_at);

DROP TABLE IF EXISTS sys_user_subject CASCADE;
CREATE TABLE sys_user_subject (
  id bigint GENERATED BY DEFAULT AS IDENTITY NOT NULL,
  user_id bigint NOT NULL,
  subject_type varchar(32) NOT NULL,
  tenant_id bigint NULL,
  subject_id bigint NULL,
  dept_id bigint NULL,
  status smallint NOT NULL DEFAULT 1,
  remark varchar(512) NULL,
  created_by bigint NULL,
  created_at timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_by bigint NULL,
  updated_at timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  CONSTRAINT pk_sys_user_subject PRIMARY KEY (id),
  CONSTRAINT chk_sys_user_subject_1 CHECK (status in (0,1)),
  CONSTRAINT chk_sys_user_subject_2 CHECK ((subject_type <> 'TENANT') or ((tenant_id is not null) and (subject_id is null))),
  CONSTRAINT chk_sys_user_subject_3 CHECK (subject_type in ('PLATFORM','TENANT','MERCHANT'))
);
COMMENT ON TABLE sys_user_subject IS '用户唯一主体身份表';
COMMENT ON COLUMN sys_user_subject.id IS '主键ID';
COMMENT ON COLUMN sys_user_subject.user_id IS '用户ID，关联sys_user.id';
COMMENT ON COLUMN sys_user_subject.subject_type IS '主体类型: PLATFORM/TENANT/MERCHANT';
COMMENT ON COLUMN sys_user_subject.tenant_id IS '租户ID；TENANT/MERCHANT主体必填';
COMMENT ON COLUMN sys_user_subject.subject_id IS '主体ID；TENANT主体为空，MERCHANT主体为商户ID';
COMMENT ON COLUMN sys_user_subject.dept_id IS '租户内部门ID；TENANT主体可填';
COMMENT ON COLUMN sys_user_subject.status IS '状态: 0禁用 1启用';
COMMENT ON COLUMN sys_user_subject.remark IS '备注';
COMMENT ON COLUMN sys_user_subject.created_by IS '创建人ID';
COMMENT ON COLUMN sys_user_subject.created_at IS '创建时间';
COMMENT ON COLUMN sys_user_subject.updated_by IS '更新人ID';
COMMENT ON COLUMN sys_user_subject.updated_at IS '更新时间';
CREATE UNIQUE INDEX uk_sys_user_subject_uk_sys_user_subject_user ON sys_user_subject (user_id);
CREATE INDEX idx_sys_user_subject_idx_sys_user_subject_type ON sys_user_subject (subject_type, status);
CREATE INDEX idx_sys_user_subject_idx_sys_user_subject_tenant ON sys_user_subject (tenant_id, status);
CREATE INDEX idx_sys_user_subject_idx_sys_user_subject_subject ON sys_user_subject (tenant_id, subject_id, status);
