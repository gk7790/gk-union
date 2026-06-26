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
-- Table structure for merchant
-- ----------------------------
DROP TABLE IF EXISTS `merchant`;
CREATE TABLE `merchant`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `merchant_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '商户号',
  `merchant_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '商户名称',
  `merchant_short_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '商户简称',
  `merchant_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'COMPANY' COMMENT '商户类型: COMPANY/PERSON',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态: 1正常 2暂停 3停用',
  `risk_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'NORMAL' COMMENT '风控状态: NORMAL/FROZEN/BLOCKED',
  `country_code` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '国家编码',
  `default_currency` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '默认币种',
  `timezone` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Asia/Shanghai' COMMENT '商户时区',
  `lang` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'zh-CN' COMMENT '商户语言',
  `contact_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '联系人',
  `contact_email` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '联系邮箱',
  `contact_phone` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '联系电话',
  `tg_user_id` bigint NULL DEFAULT NULL COMMENT 'Telegram用户ID',
  `business_license_no` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '营业执照/注册编号',
  `settle_mode` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'MANUAL' COMMENT '结算模式: MANUAL/AUTO',
  `settle_cycle` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'T1' COMMENT '结算周期: T0/T1/TN',
  `min_settle_amount` decimal(24, 8) NULL DEFAULT NULL COMMENT '最小结算金额',
  `config_json` json NULL COMMENT '商户扩展配置JSON',
  `remark` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '备注',
  `created_by` bigint NULL DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_by` bigint NULL DEFAULT NULL COMMENT '更新人ID',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_merchant_no`(`merchant_no` ASC) USING BTREE,
  UNIQUE INDEX `uk_merchant_tenant_name`(`tenant_id` ASC, `merchant_name` ASC) USING BTREE,
  INDEX `idx_merchant_tenant_status`(`tenant_id` ASC, `status` ASC) USING BTREE,
  INDEX `idx_merchant_country_currency`(`tenant_id` ASC, `country_code` ASC, `default_currency` ASC) USING BTREE,
  INDEX `idx_merchant_risk_status`(`tenant_id` ASC, `risk_status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1990654500050675703 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '支付商户主体' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for merchant_app
-- ----------------------------
DROP TABLE IF EXISTS `merchant_app`;
CREATE TABLE `merchant_app`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `merchant_id` bigint NOT NULL COMMENT '商户ID',
  `app_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '商户应用ID，对外API身份标识',
  `app_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '应用名称',
  `app_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'API' COMMENT '应用类型: API/ADMIN/SYSTEM',
  `app_env` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'TEST' COMMENT '应用环境: TEST/PROD',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态: 1正常 2暂停 3停用',
  `sign_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'HMAC_SHA256' COMMENT '签名类型: HMAC_SHA256/RSA2',
  `encrypt_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'NONE' COMMENT '加密类型: NONE/AES/RSA',
  `api_secret` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT 'API密钥密文或密钥引用，HMAC模式使用',
  `secret_version` int NOT NULL DEFAULT 1 COMMENT '密钥版本号',
  `secret_updated_at` datetime(3) NULL DEFAULT NULL COMMENT '密钥更新时间',
  `merchant_public_key` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '商户公钥，RSA2模式使用',
  `platform_public_key` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '平台公钥快照，提供给商户验签',
  `notify_url` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '默认异步通知地址',
  `return_url` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '默认同步跳转地址',
  `ip_whitelist_json` json NULL COMMENT 'IP白名单JSON数组',
  `allowed_currency_json` json NULL COMMENT '允许币种JSON数组',
  `allowed_method_json` json NULL COMMENT '允许支付方式JSON数组',
  `rate_limit_qps` int NOT NULL DEFAULT 50 COMMENT '接口限流QPS',
  `nonce_ttl_seconds` int NOT NULL DEFAULT 300 COMMENT 'nonce防重放有效秒数',
  `config_json` json NULL COMMENT '应用扩展配置JSON',
  `remark` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '备注',
  `created_by` bigint NULL DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_by` bigint NULL DEFAULT NULL COMMENT '更新人ID',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_merchant_app_id`(`app_id` ASC) USING BTREE,
  INDEX `idx_merchant_app_merchant_status`(`tenant_id` ASC, `merchant_id` ASC, `status` ASC) USING BTREE,
  INDEX `idx_merchant_app_env`(`tenant_id` ASC, `merchant_id` ASC, `app_env` ASC) USING BTREE,
  INDEX `idx_merchant_app_tenant_status`(`tenant_id` ASC, `status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1990654600050675703 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '商户API接入应用' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for merchant_fee_rule
-- ----------------------------
DROP TABLE IF EXISTS `merchant_fee_rule`;
CREATE TABLE `merchant_fee_rule`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `merchant_id` bigint NOT NULL COMMENT '商户ID',
  `merchant_app_id` bigint NULL DEFAULT NULL COMMENT '商户应用ID，NULL表示不限应用',
  `rule_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '规则名称',
  `direction` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '支付方向: PAYIN/PAYOUT',
  `country_code` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '国家编码，NULL表示不限国家',
  `currency` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '币种',
  `method_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '平台统一支付方式编码，NULL表示不限支付方式',
  `min_amount` decimal(24, 8) NULL DEFAULT NULL COMMENT '订单最小金额',
  `max_amount` decimal(24, 8) NULL DEFAULT NULL COMMENT '订单最大金额',
  `fee_mode` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '手续费模式: RATE/FIXED/RATE_FIXED',
  `fee_rate` decimal(18, 8) NOT NULL DEFAULT 0.00000000 COMMENT '比例费率，例如0.025表示2.5%',
  `fee_fixed` decimal(24, 8) NOT NULL DEFAULT 0.00000000 COMMENT '固定手续费',
  `min_fee` decimal(24, 8) NULL DEFAULT NULL COMMENT '最低手续费',
  `max_fee` decimal(24, 8) NULL DEFAULT NULL COMMENT '最高手续费',
  `fee_bearer` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'MERCHANT' COMMENT '手续费承担方: MERCHANT/CUSTOMER',
  `settle_mode` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'DEDUCT' COMMENT '结算处理方式: DEDUCT/ADD',
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
  INDEX `idx_fee_rule_merchant`(`tenant_id` ASC, `merchant_id` ASC, `direction` ASC, `currency` ASC, `status` ASC) USING BTREE,
  INDEX `idx_fee_rule_app`(`tenant_id` ASC, `merchant_app_id` ASC, `direction` ASC, `currency` ASC, `status` ASC) USING BTREE,
  INDEX `idx_fee_rule_match`(`tenant_id` ASC, `merchant_id` ASC, `direction` ASC, `country_code` ASC, `currency` ASC, `method_code` ASC, `status` ASC, `priority` ASC) USING BTREE,
  INDEX `idx_fee_rule_order_match`(`tenant_id` ASC, `merchant_id` ASC, `merchant_app_id` ASC, `direction` ASC, `currency` ASC, `method_code` ASC, `status` ASC, `priority` ASC, `id` ASC) USING BTREE,
  INDEX `idx_fee_rule_effective`(`tenant_id` ASC, `status` ASC, `effective_at` ASC, `expire_at` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2064008047517700099 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '商户手续费规则' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for merchant_notify_record
-- ----------------------------
DROP TABLE IF EXISTS `merchant_notify_record`;
CREATE TABLE `merchant_notify_record`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `notify_task_id` bigint NOT NULL COMMENT '通知任务ID',
  `task_no` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '通知任务编号',
  `attempt_no` int NOT NULL COMMENT '第几次通知',
  `notify_url` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '通知地址',
  `http_method` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'POST' COMMENT 'HTTP方法',
  `content_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'application/json' COMMENT '请求Content-Type',
  `request_signature` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '请求签名',
  `request_headers_json` json NULL COMMENT '请求头JSON',
  `request_body` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '请求体',
  `response_headers_json` json NULL COMMENT '响应头JSON',
  `response_status` int NULL DEFAULT NULL COMMENT 'HTTP响应状态码',
  `response_body` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '响应体',
  `success` tinyint NOT NULL DEFAULT 0 COMMENT '是否成功: 0否 1是',
  `error_msg` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '错误信息',
  `cost_ms` bigint NULL DEFAULT NULL COMMENT '耗时毫秒',
  `started_at` datetime(3) NULL DEFAULT NULL COMMENT '开始通知时间',
  `finished_at` datetime(3) NULL DEFAULT NULL COMMENT '完成通知时间',
  `trace_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '链路追踪ID',
  `created_by` bigint NULL DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_by` bigint NULL DEFAULT NULL COMMENT '更新人ID',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_merchant_notify_record_attempt`(`notify_task_id` ASC, `attempt_no` ASC) USING BTREE,
  INDEX `idx_merchant_notify_record_task`(`tenant_id` ASC, `task_no` ASC) USING BTREE,
  INDEX `idx_merchant_notify_record_success`(`tenant_id` ASC, `success` ASC, `created_at` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '商户通知记录' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for merchant_notify_task
-- ----------------------------
DROP TABLE IF EXISTS `merchant_notify_task`;
CREATE TABLE `merchant_notify_task`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `merchant_id` bigint NOT NULL COMMENT '平台商户ID',
  `merchant_app_id` bigint NOT NULL COMMENT '商户应用ID',
  `app_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '商户应用ID快照',
  `task_no` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '通知任务编号',
  `biz_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '业务类型: PAY_ORDER/PAYOUT_ORDER等',
  `biz_id` bigint NOT NULL COMMENT '业务ID',
  `biz_no` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '业务编号',
  `event_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '通知事件: PAY_SUCCESS/PAYOUT_FAILED等',
  `source_event_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '来源Outbox事件ID',
  `notify_url` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '通知地址',
  `http_method` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'POST' COMMENT 'HTTP方法',
  `content_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'application/json' COMMENT '请求Content-Type',
  `charset` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'UTF-8' COMMENT '请求字符集',
  `sign_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'HMAC_SHA256' COMMENT '签名方式',
  `signature` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '通知签名',
  `payload_hash` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '通知内容哈希',
  `headers_json` json NULL COMMENT '通知请求头JSON',
  `payload_json` json NOT NULL COMMENT '通知内容JSON',
  `timeout_ms` int NOT NULL DEFAULT 5000 COMMENT 'HTTP通知超时毫秒',
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'INIT' COMMENT '状态: INIT/PROCESSING/SUCCESS/FAILED/DEAD',
  `retry_count` int NOT NULL DEFAULT 0 COMMENT '已重试次数',
  `max_retry_count` int NOT NULL DEFAULT 16 COMMENT '最大重试次数',
  `next_retry_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '下次重试时间',
  `last_http_status` int NULL DEFAULT NULL COMMENT '最后HTTP状态码',
  `last_response_body` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '最后响应体',
  `last_error_msg` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '最后错误信息',
  `last_attempt_at` datetime(3) NULL DEFAULT NULL COMMENT '最后通知尝试时间',
  `locked_by` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '锁定节点',
  `locked_at` datetime(3) NULL DEFAULT NULL COMMENT '锁定时间',
  `lock_until` datetime(3) NULL DEFAULT NULL COMMENT '锁定过期时间',
  `success_at` datetime(3) NULL DEFAULT NULL COMMENT '通知成功时间',
  `dead_at` datetime(3) NULL DEFAULT NULL COMMENT '进入死信时间',
  `trace_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '链路追踪ID',
  `remark` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '备注',
  `created_by` bigint NULL DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_by` bigint NULL DEFAULT NULL COMMENT '更新人ID',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_merchant_notify_task_no`(`tenant_id` ASC, `task_no` ASC) USING BTREE,
  UNIQUE INDEX `uk_merchant_notify_task_biz_event`(`tenant_id` ASC, `biz_type` ASC, `biz_no` ASC, `event_type` ASC) USING BTREE,
  INDEX `idx_merchant_notify_task_scan`(`tenant_id` ASC, `status` ASC, `next_retry_at` ASC, `id` ASC) USING BTREE,
  INDEX `idx_merchant_notify_task_biz`(`tenant_id` ASC, `biz_type` ASC, `biz_no` ASC) USING BTREE,
  INDEX `idx_merchant_notify_task_merchant`(`tenant_id` ASC, `merchant_id` ASC, `created_at` ASC) USING BTREE,
  INDEX `idx_merchant_notify_task_lock`(`lock_until` ASC, `locked_by` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '商户通知任务' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for merchant_request_log
-- ----------------------------
DROP TABLE IF EXISTS `merchant_request_log`;
CREATE TABLE `merchant_request_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NULL DEFAULT NULL COMMENT '租户ID',
  `merchant_id` bigint NULL DEFAULT NULL COMMENT '商户ID',
  `merchant_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '商户号快照',
  `merchant_app_id` bigint NULL DEFAULT NULL COMMENT '商户应用ID',
  `app_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '商户应用ID快照',
  `request_no` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '平台请求日志号',
  `api_path` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '请求路径',
  `api_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '接口名称',
  `http_method` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'POST' COMMENT 'HTTP方法',
  `client_ip` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '客户端IP',
  `user_agent` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT 'User-Agent',
  `request_body_hash` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '请求体哈希',
  `request_body_json` json NULL COMMENT '请求体JSON，脱敏后保存',
  `request_params_json` json NULL COMMENT '请求参数JSON，脱敏后保存',
  `sign_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '签名类型: HMAC_SHA256/MD5',
  `sign_value` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '商户提交签名，脱敏后保存',
  `sign_valid` tinyint NULL DEFAULT NULL COMMENT '验签结果: 0失败 1成功',
  `timestamp_value` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '商户提交的时间戳',
  `nonce_value` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '商户提交的nonce',
  `biz_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '业务类型: PAY_ORDER/PAYOUT_ORDER/BALANCE/QUERY',
  `biz_no` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '平台业务单号',
  `merchant_order_no` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '商户订单号',
  `response_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '响应码',
  `response_message` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '响应消息',
  `response_body_json` json NULL COMMENT '响应体JSON，脱敏或摘要后保存',
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'RECEIVED' COMMENT '处理状态: RECEIVED/SUCCESS/FAILED/REJECTED',
  `error_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '错误码',
  `error_message` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '错误信息',
  `cost_ms` bigint NULL DEFAULT NULL COMMENT '处理耗时毫秒',
  `trace_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '链路追踪ID',
  `created_by` bigint NULL DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_merchant_request_no`(`request_no` ASC) USING BTREE,
  INDEX `idx_merchant_request_app`(`tenant_id` ASC, `merchant_app_id` ASC, `created_at` ASC) USING BTREE,
  INDEX `idx_merchant_request_merchant`(`tenant_id` ASC, `merchant_id` ASC, `created_at` ASC) USING BTREE,
  INDEX `idx_merchant_request_api`(`api_path` ASC, `created_at` ASC) USING BTREE,
  INDEX `idx_merchant_request_biz`(`tenant_id` ASC, `biz_type` ASC, `biz_no` ASC) USING BTREE,
  INDEX `idx_merchant_request_order`(`tenant_id` ASC, `merchant_id` ASC, `merchant_order_no` ASC) USING BTREE,
  INDEX `idx_merchant_request_status`(`status` ASC, `created_at` ASC) USING BTREE,
  INDEX `idx_merchant_request_trace`(`trace_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2064284681147719682 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '商户请求日志' ROW_FORMAT = Dynamic;

