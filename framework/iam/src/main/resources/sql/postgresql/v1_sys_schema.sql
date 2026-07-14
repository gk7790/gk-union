/*
 Navicat Premium Dump SQL

 Source Server         : 涓汉鏁版嵁搴?HK)
 Source Server Type    : MySQL
 Source Server Version : 80036 (8.0.36)
 Source Host           : rm-j6c0gts524084546n5o.mysql.rds.aliyuncs.com:3306
 Source Schema         : gk-union

 Target Server Type    : MySQL
 Target Server Version : 80036 (8.0.36)
 File Encoding         : 65001

 Date: 10/06/2026 12:31:23
*/


-- ----------------------------
-- Table structure for sys_dept
-- ----------------------------
DROP TABLE IF EXISTS "sys_dept";
CREATE TABLE "sys_dept"  (
  "id" bigint NOT NULL,
  "tenant_id" bigint NULL DEFAULT NULL,
  "pid" bigint NULL DEFAULT 0,
  "pids" varchar(500) NULL DEFAULT '',
  "name" varchar(50) NULL DEFAULT NULL,
  "status" SMALLINT NULL DEFAULT 1,
  "sort" INTEGER NULL DEFAULT 0,
  "remark" varchar(255) NULL DEFAULT NULL,
  "created_by" bigint NULL DEFAULT NULL,
  "created_at" TIMESTAMP NULL DEFAULT NULL,
  "updated_by" bigint NULL DEFAULT NULL,
  "updated_at" TIMESTAMP NULL DEFAULT NULL,
  PRIMARY KEY ("id"),
);
CREATE INDEX "idx_pid" ON "sys_dept" ("pid" ASC);
CREATE INDEX "idx_sort" ON "sys_dept" ("sort" ASC);

-- ----------------------------
-- Table structure for sys_dict_data
-- ----------------------------
DROP TABLE IF EXISTS "sys_dict_data";
CREATE TABLE "sys_dict_data"  (
  "id" bigint NOT NULL,
  "tenant_id" bigint NULL DEFAULT NULL,
  "dict_type_id" bigint NOT NULL,
  "dict_label" varchar(255) NOT NULL,
  "i18n_key" varchar(255) NULL DEFAULT NULL,
  "dict_value" varchar(128) NULL DEFAULT NULL,
  "attr_type" varchar(255) NULL DEFAULT NULL,
  "remark" varchar(255) NULL DEFAULT NULL,
  "status" SMALLINT NULL DEFAULT 1,
  "sort" INTEGER NULL DEFAULT NULL,
  "color" varchar(255) NULL DEFAULT NULL,
  "icon" varchar(255) NULL DEFAULT NULL,
  "ext" json NULL,
  "is_default" SMALLINT NULL DEFAULT NULL,
  "created_by" bigint NULL DEFAULT NULL,
  "created_at" TIMESTAMP NULL DEFAULT NULL,
  "updated_by" bigint NULL DEFAULT NULL,
  "updated_at" TIMESTAMP NULL DEFAULT NULL,
  PRIMARY KEY ("id"),
);
CREATE UNIQUE INDEX "uk_dict_type_value" ON "sys_dict_data" ("dict_type_id" ASC, "dict_value" ASC);
CREATE INDEX "idx_sort" ON "sys_dict_data" ("sort" ASC);

-- ----------------------------
-- Table structure for sys_dict_type
-- ----------------------------
DROP TABLE IF EXISTS "sys_dict_type";
CREATE TABLE "sys_dict_type"  (
  "id" bigint NOT NULL,
  "tenant_id" bigint NULL DEFAULT NULL,
  "dict_type" varchar(100) NOT NULL,
  "dict_name" varchar(255) NOT NULL,
  "remark" varchar(255) NULL DEFAULT NULL,
  "sort" INTEGER NULL DEFAULT 0,
  "scope" varchar(255) NULL DEFAULT NULL,
  "created_by" bigint NULL DEFAULT NULL,
  "created_at" TIMESTAMP NULL DEFAULT NULL,
  "updated_by" bigint NULL DEFAULT NULL,
  "updated_at" TIMESTAMP NULL DEFAULT NULL,
  PRIMARY KEY ("id"),
);
CREATE UNIQUE INDEX "dict_type" ON "sys_dict_type" ("dict_type" ASC);

-- ----------------------------
-- Table structure for sys_i18n
-- ----------------------------
DROP TABLE IF EXISTS "sys_i18n";
CREATE TABLE "sys_i18n"  (
  "id" bigint NOT NULL,
  "tenant_id" bigint NULL DEFAULT NULL,
  "biz_type" varchar(64) NOT NULL,
  "biz_id" bigint NULL DEFAULT NULL,
  "i18n_key" varchar(128) NOT NULL,
  "lang" varchar(16) NOT NULL,
  "value" varchar(255) NULL DEFAULT NULL,
  "created_by" bigint NOT NULL,
  "created_at" TIMESTAMP NOT NULL,
  "updated_by" bigint NOT NULL,
  "updated_at" TIMESTAMP NOT NULL,
  PRIMARY KEY ("id")
);

-- ----------------------------
-- Table structure for sys_language
-- ----------------------------
DROP TABLE IF EXISTS "sys_language";
CREATE TABLE "sys_language"  (
  "table_id" bigint NOT NULL,
  "table_name" varchar(32) NOT NULL,
  "field_name" varchar(32) NOT NULL,
  "field_value" varchar(200) NOT NULL,
  "language" varchar(10) NOT NULL,
  PRIMARY KEY ("table_id", "table_name", "field_name", "language"),
);
CREATE INDEX "idx_table_id" ON "sys_language" ("table_id" ASC);

-- ----------------------------
-- Table structure for sys_log_error
-- ----------------------------
DROP TABLE IF EXISTS "sys_log_error";
CREATE TABLE "sys_log_error"  (
  "id" bigint NOT NULL,
  "trace_id" varchar(64) NULL DEFAULT NULL,
  "tenant_id" bigint NULL DEFAULT NULL,
  "user_id" bigint NULL DEFAULT NULL,
  "username" varchar(64) NULL DEFAULT NULL,
  "module" varchar(64) NULL DEFAULT NULL,
  "service_name" varchar(64) NULL DEFAULT NULL,
  "error_type" varchar(64) NULL DEFAULT NULL,
  "error_code" varchar(64) NULL DEFAULT NULL,
  "error_message" text NULL,
  "stack_trace" longtext NULL,
  "stack_hash" varchar(64) NULL DEFAULT NULL,
  "request_uri" varchar(500) NULL DEFAULT NULL,
  "request_method" varchar(16) NULL DEFAULT NULL,
  "request_params" longtext NULL,
  "request_body" longtext NULL,
  "request_headers" longtext NULL,
  "ip" varchar(64) NULL DEFAULT NULL,
  "country" varchar(64) NULL DEFAULT NULL,
  "province" varchar(64) NULL DEFAULT NULL,
  "city" varchar(64) NULL DEFAULT NULL,
  "user_agent" varchar(1000) NULL DEFAULT NULL,
  "device_type" varchar(32) NULL DEFAULT NULL,
  "os" varchar(64) NULL DEFAULT NULL,
  "browser" varchar(64) NULL DEFAULT NULL,
  "http_status" int NULL DEFAULT NULL,
  "level" varchar(16) NULL DEFAULT NULL,
  "resolved" SMALLINT NULL DEFAULT 0,
  "alarmed" SMALLINT NULL DEFAULT 0,
  "env" varchar(32) NULL DEFAULT NULL,
  "created_at" TIMESTAMP NULL DEFAULT NULL,
  PRIMARY KEY ("id"),
);
CREATE INDEX "idx_trace_id" ON "sys_log_error" ("trace_id" ASC);
CREATE INDEX "idx_tenant_id" ON "sys_log_error" ("tenant_id" ASC);
CREATE INDEX "idx_user_id" ON "sys_log_error" ("user_id" ASC);
CREATE INDEX "idx_error_type" ON "sys_log_error" ("error_type" ASC);
CREATE INDEX "idx_stack_hash" ON "sys_log_error" ("stack_hash" ASC);
CREATE INDEX "idx_created_at" ON "sys_log_error" ("created_at" ASC);

-- ----------------------------
-- Table structure for sys_log_login
-- ----------------------------
DROP TABLE IF EXISTS "sys_log_login";
CREATE TABLE "sys_log_login"  (
  "id" bigint NOT NULL,
  "operation" SMALLINT UNSIGNED NULL DEFAULT NULL,
  "status" SMALLINT UNSIGNED NOT NULL,
  "user_agent" varchar(500) NULL DEFAULT NULL,
  "ip" varchar(32) NULL DEFAULT NULL,
  "created_by_name" varchar(50) NULL DEFAULT NULL,
  "created_by" bigint NULL DEFAULT NULL,
  "created_at" TIMESTAMP NULL DEFAULT NULL,
  PRIMARY KEY ("id"),
);
CREATE INDEX "idx_status" ON "sys_log_login" ("status" ASC);
CREATE INDEX "idx_created_at" ON "sys_log_login" ("created_at" ASC);

-- ----------------------------
-- Table structure for sys_log_operation
-- ----------------------------
DROP TABLE IF EXISTS "sys_log_operation";
CREATE TABLE "sys_log_operation"  (
  "id" bigint NOT NULL,
  "operation" varchar(50) NULL DEFAULT NULL,
  "request_uri" varchar(200) NULL DEFAULT NULL,
  "request_method" varchar(20) NULL DEFAULT NULL,
  "request_params" mediumtext NULL,
  "request_time" INTEGER NOT NULL,
  "user_agent" varchar(500) NULL DEFAULT NULL,
  "ip" varchar(32) NULL DEFAULT NULL,
  "status" SMALLINT UNSIGNED NOT NULL,
  "created_by_name" varchar(50) NULL DEFAULT NULL,
  "created_by" bigint NULL DEFAULT NULL,
  "created_at" TIMESTAMP NULL DEFAULT NULL,
  PRIMARY KEY ("id"),
);
CREATE INDEX "idx_created_at" ON "sys_log_operation" ("created_at" ASC);

-- ----------------------------
-- Table structure for sys_menu
-- ----------------------------
DROP TABLE IF EXISTS "sys_menu";
CREATE TABLE "sys_menu"  (
  "id" bigint NOT NULL,
  "pid" bigint NULL DEFAULT 0,
  "name" varchar(255) NULL DEFAULT NULL,
  "path" varchar(200) NULL DEFAULT NULL,
  "type" varchar(255) NULL DEFAULT NULL,
  "status" SMALLINT NULL DEFAULT 1,
  "auth_code" varchar(500) NULL DEFAULT NULL,
  "active_path" varchar(255) NULL DEFAULT NULL,
  "component" varchar(255) NULL DEFAULT NULL,
  "meta" json NULL,
  "sort" int NULL DEFAULT 0,
  "redirect" varchar(255) NULL DEFAULT NULL,
  "subject_types" json NULL,
  "domain" json NULL,
  "created_by" bigint NULL DEFAULT NULL,
  "created_at" TIMESTAMP NULL DEFAULT NULL,
  "updated_by" bigint NULL DEFAULT NULL,
  "updated_at" TIMESTAMP NULL DEFAULT NULL,
  PRIMARY KEY ("id"),
);
CREATE UNIQUE INDEX "unq_name" ON "sys_menu" ("name" ASC);
CREATE INDEX "idx_pid" ON "sys_menu" ("pid" ASC);
CREATE INDEX "idx_sort" ON "sys_menu" ("sort" ASC);

-- ----------------------------
-- Table structure for sys_params
-- ----------------------------
DROP TABLE IF EXISTS "sys_params";
CREATE TABLE "sys_params"  (
  "id" bigint NOT NULL,
  "param_code" varchar(32) NULL DEFAULT NULL,
  "param_value" varchar(2000) NULL DEFAULT NULL,
  "param_type" SMALLINT UNSIGNED NULL DEFAULT 1,
  "remark" varchar(200) NULL DEFAULT NULL,
  "created_by" bigint NULL DEFAULT NULL,
  "created_at" TIMESTAMP NULL DEFAULT NULL,
  "updated_by" bigint NULL DEFAULT NULL,
  "updated_at" TIMESTAMP NULL DEFAULT NULL,
  PRIMARY KEY ("id"),
);
CREATE UNIQUE INDEX "uk_param_code" ON "sys_params" ("param_code" ASC);
CREATE INDEX "idx_created_at" ON "sys_params" ("created_at" ASC);

-- ----------------------------
-- Table structure for sys_region
-- ----------------------------
DROP TABLE IF EXISTS "sys_region";
CREATE TABLE "sys_region"  (
  "id" bigint NOT NULL,
  "pid" bigint NULL DEFAULT NULL,
  "name" varchar(100) NULL DEFAULT NULL,
  "tree_level" SMALLINT NULL DEFAULT NULL,
  "leaf" SMALLINT NULL DEFAULT NULL,
  "sort" bigint NULL DEFAULT NULL,
  "created_by" bigint NULL DEFAULT NULL,
  "created_at" TIMESTAMP NULL DEFAULT NULL,
  "updated_by" bigint NULL DEFAULT NULL,
  "updated_at" TIMESTAMP NULL DEFAULT NULL,
  PRIMARY KEY ("id")
);

-- ----------------------------
-- Table structure for sys_role
-- ----------------------------
DROP TABLE IF EXISTS "sys_role";
CREATE TABLE "sys_role"  (
  "id" bigint NOT NULL,
  "tenant_id" bigint NOT NULL DEFAULT 0,
  "dept_id" bigint NOT NULL DEFAULT 0,
  "name" varchar(50) NULL DEFAULT NULL,
  "auth" varchar(255) NULL DEFAULT NULL,
  "role_scope" varchar(32) NOT NULL DEFAULT 'TENANT',
  "data_scope" varchar(32) NOT NULL DEFAULT 'SELF',
  "status" SMALLINT NULL DEFAULT 1,
  "remark" varchar(100) NULL DEFAULT NULL,
  "created_by" bigint NULL DEFAULT NULL,
  "created_at" TIMESTAMP NULL DEFAULT NULL,
  "updated_by" bigint NULL DEFAULT NULL,
  "updated_at" TIMESTAMP NULL DEFAULT NULL,
  PRIMARY KEY ("id"),
  CONSTRAINT "chk_sys_role_scope" CHECK ("role_scope" in ('PLATFORM','TENANT','MERCHANT')),
  CONSTRAINT "chk_sys_role_data_scope" CHECK ("data_scope" in ('ALL','TENANT_ALL','SELF_AND_CHILDREN','SELF'))
);
CREATE UNIQUE INDEX "uk_sys_role_scope_auth" ON "sys_role" ("tenant_id" ASC, "role_scope" ASC, "auth" ASC);
CREATE INDEX "idx_dept_id" ON "sys_role" ("dept_id" ASC);
CREATE INDEX "idx_sys_role_scope_status" ON "sys_role" ("role_scope" ASC, "status" ASC);

-- ----------------------------
-- Table structure for sys_role_data_scope
-- ----------------------------
DROP TABLE IF EXISTS "sys_role_data_scope";
CREATE TABLE "sys_role_data_scope"  (
  "id" bigint NOT NULL,
  "role_id" bigint NULL DEFAULT NULL,
  "dept_id" bigint NULL DEFAULT NULL,
  "created_by" bigint NULL DEFAULT NULL,
  "created_at" TIMESTAMP NULL DEFAULT NULL,
  PRIMARY KEY ("id"),
);
CREATE INDEX "idx_role_id" ON "sys_role_data_scope" ("role_id" ASC);

-- ----------------------------
-- Table structure for sys_role_menu
-- ----------------------------
DROP TABLE IF EXISTS "sys_role_menu";
CREATE TABLE "sys_role_menu"  (
  "id" bigint NOT NULL,
  "role_id" bigint NULL DEFAULT NULL,
  "menu_id" bigint NULL DEFAULT NULL,
  "created_by" bigint NULL DEFAULT NULL,
  "created_at" TIMESTAMP NULL DEFAULT NULL,
  PRIMARY KEY ("id"),
);
CREATE INDEX "idx_role_id" ON "sys_role_menu" ("role_id" ASC);
CREATE INDEX "idx_menu_id" ON "sys_role_menu" ("menu_id" ASC);

-- ----------------------------
-- Table structure for sys_role_user
-- ----------------------------
DROP TABLE IF EXISTS "sys_role_user";
CREATE TABLE "sys_role_user"  (
  "id" bigint NOT NULL,
  "user_subject_id" bigint NULL DEFAULT NULL,
  "user_id" bigint NULL DEFAULT NULL,
  "role_id" bigint NULL DEFAULT NULL,
  "created_by" bigint NULL DEFAULT NULL,
  "created_at" TIMESTAMP NULL DEFAULT NULL,
  PRIMARY KEY ("id"),
);
CREATE UNIQUE INDEX "uk_sys_role_user_subject_role" ON "sys_role_user" ("user_subject_id" ASC, "role_id" ASC);
CREATE INDEX "idx_user_subject_id" ON "sys_role_user" ("user_subject_id" ASC);
CREATE INDEX "idx_role_id" ON "sys_role_user" ("role_id" ASC);
CREATE INDEX "idx_user_id" ON "sys_role_user" ("user_id" ASC);

-- ----------------------------
-- Table structure for sys_tenant
-- ----------------------------
DROP TABLE IF EXISTS "sys_tenant";
CREATE TABLE "sys_tenant"  (
  "id" bigint NULL DEFAULT NULL,
  "name" varchar(255) NULL DEFAULT NULL,
  "code" varchar(100) NULL DEFAULT NULL,
  "status" SMALLINT NULL DEFAULT 1,
  "domain" varchar(255) NULL DEFAULT NULL,
  "currency" varchar(20) NULL DEFAULT NULL,
  "timezone" varchar(50) NULL DEFAULT 'UTC',
  "lang" varchar(255) NULL DEFAULT 'en-US',
  "api_key" varchar(255) NULL DEFAULT NULL,
  "api_secret" varchar(255) NULL DEFAULT NULL,
  "remark" varchar(255) NULL DEFAULT NULL,
  "created_by" bigint NULL DEFAULT NULL,
  "created_at" TIMESTAMP NULL DEFAULT NULL,
  "updated_by" bigint NULL DEFAULT NULL,
  "updated_at" TIMESTAMP NULL DEFAULT NULL
);

-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
DROP TABLE IF EXISTS "sys_user";
CREATE TABLE "sys_user"  (
  "id" bigint NOT NULL,
  "nickname" varchar(255) NULL DEFAULT NULL,
  "username" varchar(50) NOT NULL,
  "password" varchar(100) NOT NULL,
  "real_name" varchar(50) NULL DEFAULT NULL,
  "avatar" varchar(200) NULL DEFAULT NULL,
  "gender" SMALLINT UNSIGNED NULL DEFAULT NULL,
  "email" varchar(100) NULL DEFAULT NULL,
  "mobile" varchar(100) NULL DEFAULT NULL,
  "status" SMALLINT NOT NULL DEFAULT 1,
  "remark" varchar(255) NULL DEFAULT NULL,
  "created_by" bigint NULL DEFAULT NULL,
  "created_at" TIMESTAMP NULL DEFAULT NULL,
  "updated_by" bigint NULL DEFAULT NULL,
  "updated_at" TIMESTAMP NULL DEFAULT NULL,
  "auth_type" SMALLINT NULL DEFAULT NULL,
  "auth_secret" varchar(255) NULL DEFAULT NULL,
  PRIMARY KEY ("id"),
);
CREATE UNIQUE INDEX "uk_username" ON "sys_user" ("username" ASC);
CREATE UNIQUE INDEX "uk_sys_user_mobile" ON "sys_user" ("mobile" ASC);
CREATE UNIQUE INDEX "uk_sys_user_email" ON "sys_user" ("email" ASC);
CREATE INDEX "idx_created_at" ON "sys_user" ("created_at" ASC);

-- ----------------------------
-- Table structure for sys_user_subject
-- ----------------------------
DROP TABLE IF EXISTS "sys_user_subject";
CREATE TABLE "sys_user_subject"  (
  "id" BIGINT GENERATED BY DEFAULT AS IDENTITY,
  "user_id" bigint NOT NULL,
  "subject_type" varchar(32) NOT NULL,
  "tenant_id" bigint NULL DEFAULT NULL,
  "merchant_id" bigint NULL DEFAULT NULL,
  "dept_id" bigint NULL DEFAULT NULL,
  "status" SMALLINT NOT NULL DEFAULT 1,
  "remark" varchar(512) NULL DEFAULT NULL,
  "created_by" bigint NULL DEFAULT NULL,
  "created_at" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  "updated_by" bigint NULL DEFAULT NULL,
  "updated_at" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY ("id"),
  CONSTRAINT "chk_sys_user_subject_type" CHECK ("subject_type" in ('PLATFORM','TENANT','MERCHANT')),
  CONSTRAINT "chk_sys_user_subject_status" CHECK ("status" in (1,2,3)),
  CONSTRAINT "chk_sys_user_subject_platform" CHECK ("subject_type" <> 'PLATFORM' OR ("tenant_id" is null AND "merchant_id" is null)),
  CONSTRAINT "chk_sys_user_subject_tenant" CHECK ("subject_type" <> 'TENANT' OR ("tenant_id" is not null AND "merchant_id" is null)),
  CONSTRAINT "chk_sys_user_subject_merchant" CHECK ("subject_type" <> 'MERCHANT' OR ("tenant_id" is not null AND "merchant_id" is not null))
);
CREATE UNIQUE INDEX "uk_sys_user_subject_user" ON "sys_user_subject" ("user_id" ASC);
CREATE INDEX "idx_sys_user_subject_type" ON "sys_user_subject" ("subject_type" ASC, "status" ASC);
CREATE INDEX "idx_sys_user_subject_tenant" ON "sys_user_subject" ("tenant_id" ASC, "status" ASC);
CREATE INDEX "idx_sys_user_subject_merchant" ON "sys_user_subject" ("tenant_id" ASC, "merchant_id" ASC, "status" ASC);

