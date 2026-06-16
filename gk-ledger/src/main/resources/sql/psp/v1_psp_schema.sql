/*
 Navicat Premium Dump SQL

 Source Server         : 个人数据库(HK)
 Source Server Type    : MySQL
 Source Server Version : 80036 (8.0.36)
 Source Host           : rm-j6c0gts524084546n5o.mysql.rds.aliyuncs.com:3306
 Source Schema         : gk-union

 Target Server Type    : MySQL
 Target Server Version : 80036 (8.0.36)
 File Encoding         : 65001

 Date: 10/06/2026 12:31:23
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for psp_account
-- ----------------------------
DROP TABLE IF EXISTS `psp_account`;
CREATE TABLE `psp_account`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `psp_id` bigint NOT NULL COMMENT 'PSP ID',
  `psp_account_no` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'PSP账户号/商户号',
  `psp_account_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT 'PSP账户名称',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态: 1正常 2暂停 3停用',
  `secret_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'HMAC' COMMENT '密钥类型: HMAC/BASIC/TOKEN',
  `api_key` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT 'PSP API Key或Key引用',
  `api_secret` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT 'PSP API Secret密文或密钥引用',
  `callback_secret` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '回调验签密钥密文或密钥引用',
  `config_json` json NULL COMMENT 'PSP账户扩展配置JSON',
  `remark` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '备注',
  `created_by` bigint NULL DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_by` bigint NULL DEFAULT NULL COMMENT '更新人ID',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_psp_account_scope`(`tenant_id` ASC, `psp_id` ASC, `psp_account_no` ASC) USING BTREE,
  INDEX `idx_psp_account_tenant`(`tenant_id` ASC, `psp_id` ASC, `status` ASC) USING BTREE,
  INDEX `idx_psp_account_no`(`psp_id` ASC, `psp_account_no` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2063994839394340866 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'PSP账户配置' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for psp_callback_log
-- ----------------------------
DROP TABLE IF EXISTS `psp_callback_log`;
CREATE TABLE `psp_callback_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `merchant_id` bigint NULL DEFAULT NULL COMMENT '平台商户ID',
  `psp_id` bigint NOT NULL COMMENT 'PSP ID',
  `psp_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'PSP编码快照',
  `biz_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '业务类型: PAY_ORDER/PAYOUT_ORDER/REFUND等',
  `biz_id` bigint NULL DEFAULT NULL COMMENT '业务ID',
  `biz_no` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '业务编号',
  `psp_order_no` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT 'PSP订单号',
  `callback_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '回调类型',
  `callback_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT 'PSP回调ID',
  `callback_key` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '回调幂等键，有callback_id时使用PSP回调ID，否则使用callback_type+body_hash',
  `body_hash` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '回调报文哈希，用于无callback_id时幂等',
  `headers_json` json NULL COMMENT '回调请求头JSON',
  `body_json` json NULL COMMENT '回调请求体JSON',
  `raw_body` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '原始回调体',
  `signature` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '回调签名',
  `verify_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'INIT' COMMENT '验签状态: INIT/SUCCESS/FAILED/SKIPPED',
  `process_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'INIT' COMMENT '处理状态: INIT/SUCCESS/FAILED/IGNORED',
  `error_msg` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '错误信息',
  `received_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '接收时间',
  `processed_at` datetime(3) NULL DEFAULT NULL COMMENT '处理时间',
  `trace_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '链路追踪ID',
  `created_by` bigint NULL DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_by` bigint NULL DEFAULT NULL COMMENT '更新人ID',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_psp_callback_key`(`psp_id` ASC, `callback_key` ASC) USING BTREE,
  INDEX `idx_psp_callback_biz`(`tenant_id` ASC, `biz_type` ASC, `biz_no` ASC) USING BTREE,
  INDEX `idx_psp_callback_order`(`psp_id` ASC, `psp_order_no` ASC) USING BTREE,
  INDEX `idx_psp_callback_process`(`process_status` ASC, `received_at` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'PSP回调日志' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for psp_fee_rule
-- ----------------------------
DROP TABLE IF EXISTS `psp_fee_rule`;
CREATE TABLE `psp_fee_rule`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `psp_id` bigint NOT NULL COMMENT 'PSP ID',
  `psp_account_id` bigint NULL DEFAULT NULL COMMENT 'PSP账户配置ID，NULL表示不限账户',
  `psp_method_id` bigint NULL DEFAULT NULL COMMENT 'PSP支付方式ID，NULL表示不限PSP支付方式',
  `rule_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '规则名称',
  `direction` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '方向: PAYIN/PAYOUT',
  `country_code` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '国家编码，NULL表示不限国家',
  `currency` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '币种',
  `method_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '平台统一支付方式编码，NULL表示不限支付方式',
  `min_amount` decimal(24, 8) NULL DEFAULT NULL COMMENT '订单最小金额',
  `max_amount` decimal(24, 8) NULL DEFAULT NULL COMMENT '订单最大金额',
  `fee_mode` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '手续费模式: RATE/FIXED/RATE_FIXED',
  `fee_rate` decimal(18, 8) NOT NULL DEFAULT 0.00000000 COMMENT '比例费率，例如0.012表示1.2%',
  `fee_fixed` decimal(24, 8) NOT NULL DEFAULT 0.00000000 COMMENT '固定手续费',
  `min_fee` decimal(24, 8) NULL DEFAULT NULL COMMENT '最低手续费',
  `max_fee` decimal(24, 8) NULL DEFAULT NULL COMMENT '最高手续费',
  `priority` int NOT NULL DEFAULT 100 COMMENT '优先级，数字越小越优先',
  `effective_at` datetime(3) NULL DEFAULT NULL COMMENT '生效时间',
  `expire_at` datetime(3) NULL DEFAULT NULL COMMENT '失效时间',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态: 1正常 2暂停 3停用',
  `remark` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '备注',
  `created_by` bigint NULL DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_by` bigint NULL DEFAULT NULL COMMENT '更新人ID',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_psp_fee_rule_psp`(`tenant_id` ASC, `psp_id` ASC, `direction` ASC, `currency` ASC, `status` ASC) USING BTREE,
  INDEX `idx_psp_fee_rule_account`(`tenant_id` ASC, `psp_account_id` ASC, `direction` ASC, `currency` ASC, `status` ASC) USING BTREE,
  INDEX `idx_psp_fee_rule_method`(`tenant_id` ASC, `psp_method_id` ASC, `direction` ASC, `currency` ASC, `status` ASC) USING BTREE,
  INDEX `idx_psp_fee_rule_match`(`tenant_id` ASC, `psp_id` ASC, `psp_account_id` ASC, `psp_method_id` ASC, `country_code` ASC, `currency` ASC, `method_code` ASC, `direction` ASC, `status` ASC, `priority` ASC) USING BTREE,
  INDEX `idx_psp_fee_rule_effective`(`tenant_id` ASC, `status` ASC, `effective_at` ASC, `expire_at` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2064008047517700099 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'PSP成本手续费规则' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for psp_method
-- ----------------------------
DROP TABLE IF EXISTS `psp_method`;
CREATE TABLE `psp_method`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `psp_id` bigint NOT NULL COMMENT 'PSP ID',
  `psp_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'PSP编码快照',
  `method_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '平台统一支付方式编码',
  `psp_method_code` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'PSP支付方式编码',
  `method_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '支付方式名称',
  `country_code` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '国家编码',
  `currency` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '币种',
  `direction` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '方向: PAYIN/PAYOUT',
  `min_amount` decimal(24, 8) NULL DEFAULT NULL COMMENT '最小金额',
  `max_amount` decimal(24, 8) NULL DEFAULT NULL COMMENT '最大金额',
  `daily_limit` decimal(24, 8) NULL DEFAULT NULL COMMENT '日限额',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态: 1正常 2暂停 3停用',
  `config_json` json NULL COMMENT 'PSP支付方式扩展配置JSON',
  `remark` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '备注',
  `created_by` bigint NULL DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_by` bigint NULL DEFAULT NULL COMMENT '更新人ID',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_psp_method_scope`(`psp_id` ASC, `country_code` ASC, `currency` ASC, `method_code` ASC, `direction` ASC) USING BTREE,
  INDEX `idx_psp_method_match`(`country_code` ASC, `currency` ASC, `method_code` ASC, `direction` ASC, `status` ASC) USING BTREE,
  INDEX `idx_psp_method_psp`(`psp_id` ASC, `status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2063998614079238147 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'PSP支付方式映射' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for psp_provider
-- ----------------------------
DROP TABLE IF EXISTS `psp_provider`;
CREATE TABLE `psp_provider`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `psp_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'PSP编码',
  `psp_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'PSP名称',
  `country_code` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '主要国家编码',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态: 1正常 2暂停 3停用',
  `base_url` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '基础URL',
  `api_version` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT 'API版本',
  `support_payin` tinyint NOT NULL DEFAULT 1 COMMENT '是否支持代收: 0否 1是',
  `support_payout` tinyint NOT NULL DEFAULT 1 COMMENT '是否支持代付: 0否 1是',
  `config_json` json NULL COMMENT 'PSP扩展配置JSON',
  `remark` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '备注',
  `created_by` bigint NULL DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_by` bigint NULL DEFAULT NULL COMMENT '更新人ID',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_psp_provider_code`(`psp_code` ASC) USING BTREE,
  INDEX `idx_psp_provider_country_status`(`country_code` ASC, `status` ASC) USING BTREE,
  INDEX `idx_psp_provider_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2063997149537304578 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'PSP三方支付公司' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for psp_request_log
-- ----------------------------
DROP TABLE IF EXISTS `psp_request_log`;
CREATE TABLE `psp_request_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `merchant_id` bigint NULL DEFAULT NULL COMMENT '平台商户ID',
  `psp_id` bigint NOT NULL COMMENT 'PSP ID',
  `psp_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'PSP编码快照',
  `biz_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '业务类型: PAY_ORDER/PAYOUT_ORDER/QUERY/REFUND等',
  `biz_id` bigint NULL DEFAULT NULL COMMENT '业务ID',
  `biz_no` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '业务编号',
  `request_no` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '平台请求编号',
  `psp_request_no` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT 'PSP请求编号',
  `psp_order_no` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT 'PSP订单号',
  `request_url` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '请求URL',
  `http_method` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT 'HTTP方法',
  `request_headers_json` json NULL COMMENT '请求头JSON，敏感字段需要脱敏',
  `request_body` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '请求体，敏感字段需要脱敏',
  `response_status` int NULL DEFAULT NULL COMMENT 'HTTP响应状态码',
  `response_body` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '响应体，敏感字段需要脱敏',
  `success` tinyint NULL DEFAULT NULL COMMENT '是否成功: 0否 1是',
  `error_code` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '错误码',
  `error_msg` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '错误信息',
  `cost_ms` bigint NULL DEFAULT NULL COMMENT '耗时毫秒',
  `trace_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '链路追踪ID',
  `created_by` bigint NULL DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_by` bigint NULL DEFAULT NULL COMMENT '更新人ID',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_psp_request_no`(`request_no` ASC) USING BTREE,
  INDEX `idx_psp_request_biz`(`tenant_id` ASC, `biz_type` ASC, `biz_no` ASC) USING BTREE,
  INDEX `idx_psp_request_psp_order`(`psp_id` ASC, `psp_order_no` ASC) USING BTREE,
  INDEX `idx_psp_request_created`(`tenant_id` ASC, `created_at` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2064284677200879618 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'PSP请求响应日志' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for psp_route_rule
-- ----------------------------
DROP TABLE IF EXISTS `psp_route_rule`;
CREATE TABLE `psp_route_rule`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `merchant_id` bigint NULL DEFAULT NULL COMMENT '平台商户ID，NULL表示租户级规则',
  `merchant_app_id` bigint NULL DEFAULT NULL COMMENT '商户应用ID，NULL表示不限应用',
  `route_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '路由规则名称',
  `route_mode` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PRIORITY' COMMENT '路由模式: PRIORITY/WEIGHT',
  `country_code` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '国家编码',
  `currency` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '币种',
  `method_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '平台统一支付方式编码',
  `direction` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '方向: PAYIN/PAYOUT',
  `psp_id` bigint NOT NULL COMMENT 'PSP ID',
  `psp_method_id` bigint NOT NULL COMMENT 'PSP支付方式ID',
  `psp_account_id` bigint NOT NULL COMMENT 'PSP账户配置ID',
  `priority` int NOT NULL DEFAULT 100 COMMENT '优先级，数值越小越优先',
  `weight` int NOT NULL DEFAULT 100 COMMENT '权重，V1可暂不使用',
  `min_amount` decimal(24, 8) NULL DEFAULT NULL COMMENT '最小金额',
  `max_amount` decimal(24, 8) NULL DEFAULT NULL COMMENT '最大金额',
  `start_time` time NULL DEFAULT NULL COMMENT '每日开始时间',
  `end_time` time NULL DEFAULT NULL COMMENT '每日结束时间',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态: 1正常 2暂停 3停用',
  `remark` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '备注',
  `created_by` bigint NULL DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_by` bigint NULL DEFAULT NULL COMMENT '更新人ID',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_psp_route_match`(`tenant_id` ASC, `country_code` ASC, `currency` ASC, `method_code` ASC, `direction` ASC, `status` ASC, `priority` ASC) USING BTREE,
  INDEX `idx_psp_route_merchant`(`tenant_id` ASC, `merchant_id` ASC, `merchant_app_id` ASC, `status` ASC) USING BTREE,
  INDEX `idx_psp_route_psp`(`psp_id` ASC, `psp_method_id` ASC, `psp_account_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1990654400050675714 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'PSP路由规则' ROW_FORMAT = Dynamic;

