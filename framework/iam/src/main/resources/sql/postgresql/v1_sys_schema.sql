-- gk-union PostgreSQL schema generated from the provided MySQL dump.
-- Source: pasted-text.txt. MySQL comments and Navicat metadata are intentionally omitted.

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

DROP TABLE IF EXISTS sys_language CASCADE;
CREATE TABLE sys_language (
  table_id bigint NOT NULL,
  table_name varchar(32) NOT NULL,
  field_name varchar(32) NOT NULL,
  field_value varchar(200) NOT NULL,
  language varchar(10) NOT NULL,
  CONSTRAINT pk_sys_language PRIMARY KEY (table_id, table_name, field_name, language)
);
CREATE INDEX idx_sys_language_idx_table_id ON sys_language (table_id);

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
  merchant_id bigint NULL,
  dept_id bigint NULL,
  status smallint NOT NULL DEFAULT 1,
  remark varchar(512) NULL,
  created_by bigint NULL,
  created_at timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_by bigint NULL,
  updated_at timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  CONSTRAINT pk_sys_user_subject PRIMARY KEY (id),
  CONSTRAINT chk_sys_user_subject_1 CHECK (status in (0,1)),
  CONSTRAINT chk_sys_user_subject_2 CHECK ((subject_type <> 'TENANT') or ((tenant_id is not null) and (merchant_id is null))),
  CONSTRAINT chk_sys_user_subject_3 CHECK (subject_type in ('PLATFORM','TENANT','MERCHANT'))
);
CREATE UNIQUE INDEX uk_sys_user_subject_uk_sys_user_subject_user ON sys_user_subject (user_id);
CREATE INDEX idx_sys_user_subject_idx_sys_user_subject_type ON sys_user_subject (subject_type, status);
CREATE INDEX idx_sys_user_subject_idx_sys_user_subject_tenant ON sys_user_subject (tenant_id, status);
CREATE INDEX idx_sys_user_subject_idx_sys_user_subject_merchant ON sys_user_subject (tenant_id, merchant_id, status);
