-- gk-union PostgreSQL schema generated from the provided MySQL dump.
-- Source: pasted-text.txt. Navicat metadata is intentionally omitted.

DROP TABLE IF EXISTS gen_base_class CASCADE;
CREATE TABLE gen_base_class (
  id bigint NOT NULL,
  package_name varchar(200) NULL,
  code varchar(200) NULL,
  fields varchar(500) NULL,
  remark varchar(200) NULL,
  created_at timestamp NULL,
  CONSTRAINT pk_gen_base_class PRIMARY KEY (id)
);
COMMENT ON TABLE gen_base_class IS '基类管理';
COMMENT ON COLUMN gen_base_class.id IS 'id';
COMMENT ON COLUMN gen_base_class.package_name IS '基类包名';
COMMENT ON COLUMN gen_base_class.code IS '基类编码';
COMMENT ON COLUMN gen_base_class.fields IS '基类字段，多个用英文逗号分隔';
COMMENT ON COLUMN gen_base_class.remark IS '备注';
COMMENT ON COLUMN gen_base_class.created_at IS '创建时间';

DROP TABLE IF EXISTS gen_datasource CASCADE;
CREATE TABLE gen_datasource (
  id bigint NOT NULL,
  db_type varchar(200) NULL,
  conn_name varchar(200) NOT NULL,
  conn_url varchar(500) NULL,
  username varchar(200) NULL,
  password varchar(200) NULL,
  status smallint NULL,
  created_at timestamp NULL,
  CONSTRAINT pk_gen_datasource PRIMARY KEY (id)
);
COMMENT ON TABLE gen_datasource IS '数据源管理';
COMMENT ON COLUMN gen_datasource.id IS 'id';
COMMENT ON COLUMN gen_datasource.db_type IS '数据库类型 MySQL、Oracle、SQLServer、PostgreSQL';
COMMENT ON COLUMN gen_datasource.conn_name IS '连接名';
COMMENT ON COLUMN gen_datasource.conn_url IS 'URL';
COMMENT ON COLUMN gen_datasource.username IS '用户名';
COMMENT ON COLUMN gen_datasource.password IS '密码';
COMMENT ON COLUMN gen_datasource.status IS '状态: 1正常 2暂停 3停用';
COMMENT ON COLUMN gen_datasource.created_at IS '创建时间';

DROP TABLE IF EXISTS gen_field_type CASCADE;
CREATE TABLE gen_field_type (
  id bigint NOT NULL,
  column_type varchar(128) NULL,
  attr_type varchar(200) NULL,
  ui_type varchar(255) NULL,
  package_name varchar(200) NULL,
  created_at timestamp NULL,
  CONSTRAINT pk_gen_field_type PRIMARY KEY (id)
);
COMMENT ON TABLE gen_field_type IS '字段类型管理';
COMMENT ON COLUMN gen_field_type.id IS 'id';
COMMENT ON COLUMN gen_field_type.column_type IS '字段类型';
COMMENT ON COLUMN gen_field_type.attr_type IS '属性类型';
COMMENT ON COLUMN gen_field_type.ui_type IS 'ta字段类型';
COMMENT ON COLUMN gen_field_type.package_name IS '属性包名';
COMMENT ON COLUMN gen_field_type.created_at IS '创建时间';
CREATE UNIQUE INDEX uk_gen_field_type_column_type ON gen_field_type (column_type);

DROP TABLE IF EXISTS gen_table_field CASCADE;
CREATE TABLE gen_table_field (
  id bigint NOT NULL,
  table_id bigint NULL,
  table_name varchar(200) NULL,
  column_name varchar(200) NULL,
  column_type varchar(200) NULL,
  column_comment varchar(200) NULL,
  attr_name varchar(200) NULL,
  attr_type varchar(200) NULL,
  ui_type varchar(255) NULL,
  package_name varchar(200) NULL,
  is_pk smallint NULL,
  is_required smallint NULL,
  is_form smallint NULL,
  is_list smallint NULL,
  is_query smallint NULL,
  query_type varchar(200) NULL,
  form_type varchar(200) NULL,
  dict_name varchar(200) NULL,
  validator_type varchar(200) NULL,
  sort integer NULL,
  CONSTRAINT pk_gen_table_field PRIMARY KEY (id)
);
COMMENT ON TABLE gen_table_field IS '代码生成表列';
COMMENT ON COLUMN gen_table_field.id IS 'id';
COMMENT ON COLUMN gen_table_field.table_id IS '表ID';
COMMENT ON COLUMN gen_table_field.table_name IS '表名';
COMMENT ON COLUMN gen_table_field.column_name IS '列名';
COMMENT ON COLUMN gen_table_field.column_type IS '类型';
COMMENT ON COLUMN gen_table_field.column_comment IS '列说明';
COMMENT ON COLUMN gen_table_field.attr_name IS '属性名';
COMMENT ON COLUMN gen_table_field.attr_type IS '属性类型';
COMMENT ON COLUMN gen_table_field.ui_type IS 'Ts属性类型';
COMMENT ON COLUMN gen_table_field.package_name IS '属性包名';
COMMENT ON COLUMN gen_table_field.is_pk IS '是否主键 0：否  1：是';
COMMENT ON COLUMN gen_table_field.is_required IS '是否必填 0：否  1：是';
COMMENT ON COLUMN gen_table_field.is_form IS '是否表单字段 0：否  1：是';
COMMENT ON COLUMN gen_table_field.is_list IS '是否列表字段 0：否  1：是';
COMMENT ON COLUMN gen_table_field.is_query IS '是否查询字段 0：否  1：是';
COMMENT ON COLUMN gen_table_field.query_type IS '查询方式';
COMMENT ON COLUMN gen_table_field.form_type IS '表单类型';
COMMENT ON COLUMN gen_table_field.dict_name IS '字典名称';
COMMENT ON COLUMN gen_table_field.validator_type IS '效验方式';
COMMENT ON COLUMN gen_table_field.sort IS '排序';
CREATE INDEX idx_gen_table_field_table_name ON gen_table_field (table_name);

DROP TABLE IF EXISTS gen_table_info CASCADE;
CREATE TABLE gen_table_info (
  id bigint NOT NULL,
  table_name varchar(128) NULL,
  class_name varchar(200) NULL,
  table_comment varchar(200) NULL,
  author varchar(200) NULL,
  email varchar(200) NULL,
  package_name varchar(200) NULL,
  version varchar(200) NULL,
  backend_path varchar(500) NULL,
  frontend_path varchar(500) NULL,
  module_name varchar(200) NULL,
  sub_module_name varchar(200) NULL,
  datasource_id bigint NULL,
  baseclass_id bigint NULL,
  created_at timestamp NULL,
  CONSTRAINT pk_gen_table_info PRIMARY KEY (id)
);
COMMENT ON TABLE gen_table_info IS '代码生成表';
COMMENT ON COLUMN gen_table_info.id IS 'id';
COMMENT ON COLUMN gen_table_info.table_name IS '表名';
COMMENT ON COLUMN gen_table_info.class_name IS '类名';
COMMENT ON COLUMN gen_table_info.table_comment IS '功能名';
COMMENT ON COLUMN gen_table_info.author IS '作者';
COMMENT ON COLUMN gen_table_info.email IS '邮箱';
COMMENT ON COLUMN gen_table_info.package_name IS '项目包名';
COMMENT ON COLUMN gen_table_info.version IS '项目版本号';
COMMENT ON COLUMN gen_table_info.backend_path IS '后端生成路径';
COMMENT ON COLUMN gen_table_info.frontend_path IS '前端生成路径';
COMMENT ON COLUMN gen_table_info.module_name IS '模块名';
COMMENT ON COLUMN gen_table_info.sub_module_name IS '子模块名';
COMMENT ON COLUMN gen_table_info.datasource_id IS '数据源ID';
COMMENT ON COLUMN gen_table_info.baseclass_id IS '基类ID';
COMMENT ON COLUMN gen_table_info.created_at IS '创建时间';
CREATE UNIQUE INDEX uk_gen_table_info_table_name ON gen_table_info (table_name);

DROP TABLE IF EXISTS gen_template CASCADE;
CREATE TABLE gen_template (
  id bigint NOT NULL,
  name varchar(200) NULL,
  file_name varchar(200) NULL,
  content text NOT NULL,
  path varchar(500) NULL,
  status smallint NULL,
  created_at timestamp NULL,
  CONSTRAINT pk_gen_template PRIMARY KEY (id)
);
COMMENT ON TABLE gen_template IS '模板管理';
COMMENT ON COLUMN gen_template.id IS 'id';
COMMENT ON COLUMN gen_template.name IS '名称';
COMMENT ON COLUMN gen_template.file_name IS '文件名';
COMMENT ON COLUMN gen_template.content IS '内容';
COMMENT ON COLUMN gen_template.path IS '生成路径';
COMMENT ON COLUMN gen_template.status IS '状态: 1正常 2暂停 3停用';
COMMENT ON COLUMN gen_template.created_at IS '创建时间';
