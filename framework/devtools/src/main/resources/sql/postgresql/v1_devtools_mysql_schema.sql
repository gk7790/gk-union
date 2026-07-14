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
-- Table structure for gen_base_class
-- ----------------------------
DROP TABLE IF EXISTS "gen_base_class";
CREATE TABLE "gen_base_class"  (
  "id" bigint NOT NULL,
  "package_name" varchar(200) NULL DEFAULT NULL,
  "code" varchar(200) NULL DEFAULT NULL,
  "fields" varchar(500) NULL DEFAULT NULL,
  "remark" varchar(200) NULL DEFAULT NULL,
  "created_at" TIMESTAMP NULL DEFAULT NULL,
  PRIMARY KEY ("id")
);

-- ----------------------------
-- Table structure for gen_datasource
-- ----------------------------
DROP TABLE IF EXISTS "gen_datasource";
CREATE TABLE "gen_datasource"  (
  "id" bigint NOT NULL,
  "db_type" varchar(200) NULL DEFAULT NULL,
  "conn_name" varchar(200) NOT NULL,
  "conn_url" varchar(500) NULL DEFAULT NULL,
  "username" varchar(200) NULL DEFAULT NULL,
  "password" varchar(200) NULL DEFAULT NULL,
  "status" SMALLINT NULL DEFAULT 1,
  "created_at" TIMESTAMP NULL DEFAULT NULL,
  PRIMARY KEY ("id")
);

-- ----------------------------
-- Table structure for gen_field_type
-- ----------------------------
DROP TABLE IF EXISTS "gen_field_type";
CREATE TABLE "gen_field_type"  (
  "id" bigint NOT NULL,
  "column_type" varchar(128) NULL DEFAULT NULL,
  "attr_type" varchar(200) NULL DEFAULT NULL,
  "ui_type" varchar(255) NULL DEFAULT NULL,
  "package_name" varchar(200) NULL DEFAULT NULL,
  "created_at" TIMESTAMP NULL DEFAULT NULL,
  PRIMARY KEY ("id"),
);
CREATE UNIQUE INDEX "column_type" ON "gen_field_type" ("column_type" ASC);

-- ----------------------------
-- Table structure for gen_table_field
-- ----------------------------
DROP TABLE IF EXISTS "gen_table_field";
CREATE TABLE "gen_table_field"  (
  "id" bigint NOT NULL,
  "table_id" bigint NULL DEFAULT NULL,
  "table_name" varchar(200) NULL DEFAULT NULL,
  "column_name" varchar(200) NULL DEFAULT NULL,
  "column_type" varchar(200) NULL DEFAULT NULL,
  "column_comment" varchar(200) NULL DEFAULT NULL,
  "attr_name" varchar(200) NULL DEFAULT NULL,
  "attr_type" varchar(200) NULL DEFAULT NULL,
  "ui_type" varchar(255) NULL DEFAULT NULL,
  "package_name" varchar(200) NULL DEFAULT NULL,
  "is_pk" SMALLINT NULL DEFAULT NULL,
  "is_required" SMALLINT NULL DEFAULT NULL,
  "is_form" SMALLINT NULL DEFAULT NULL,
  "is_list" SMALLINT NULL DEFAULT NULL,
  "is_query" SMALLINT NULL DEFAULT NULL,
  "query_type" varchar(200) NULL DEFAULT NULL,
  "form_type" varchar(200) NULL DEFAULT NULL,
  "dict_name" varchar(200) NULL DEFAULT NULL,
  "validator_type" varchar(200) NULL DEFAULT NULL,
  "sort" int NULL DEFAULT NULL,
  PRIMARY KEY ("id"),
);
CREATE INDEX "table_name" ON "gen_table_field" ("table_name" ASC);

-- ----------------------------
-- Table structure for gen_table_info
-- ----------------------------
DROP TABLE IF EXISTS "gen_table_info";
CREATE TABLE "gen_table_info"  (
  "id" bigint NOT NULL,
  "table_name" varchar(128) NULL DEFAULT NULL,
  "class_name" varchar(200) NULL DEFAULT NULL,
  "table_comment" varchar(200) NULL DEFAULT NULL,
  "author" varchar(200) NULL DEFAULT NULL,
  "email" varchar(200) NULL DEFAULT NULL,
  "package_name" varchar(200) NULL DEFAULT NULL,
  "version" varchar(200) NULL DEFAULT NULL,
  "backend_path" varchar(500) NULL DEFAULT NULL,
  "frontend_path" varchar(500) NULL DEFAULT NULL,
  "module_name" varchar(200) NULL DEFAULT NULL,
  "sub_module_name" varchar(200) NULL DEFAULT NULL,
  "datasource_id" bigint NULL DEFAULT NULL,
  "baseclass_id" bigint NULL DEFAULT NULL,
  "created_at" TIMESTAMP NULL DEFAULT NULL,
  PRIMARY KEY ("id"),
);
CREATE UNIQUE INDEX "table_name" ON "gen_table_info" ("table_name" ASC);

-- ----------------------------
-- Table structure for gen_template
-- ----------------------------
DROP TABLE IF EXISTS "gen_template";
CREATE TABLE "gen_template"  (
  "id" bigint NOT NULL,
  "name" varchar(200) NULL DEFAULT NULL,
  "file_name" varchar(200) NULL DEFAULT NULL,
  "content" mediumtext NOT NULL,
  "path" varchar(500) NULL DEFAULT NULL,
  "status" SMALLINT NULL DEFAULT 1,
  "created_at" TIMESTAMP NULL DEFAULT NULL,
  PRIMARY KEY ("id")
);
