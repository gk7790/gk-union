-- gk-union PostgreSQL schema generated from the provided MySQL dump.
-- Source: pasted-text.txt. MySQL comments and Navicat metadata are intentionally omitted.

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
