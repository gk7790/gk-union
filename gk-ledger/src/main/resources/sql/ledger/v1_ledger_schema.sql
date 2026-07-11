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
-- Table structure for ledger_account
-- ----------------------------
DROP TABLE IF EXISTS `ledger_account`;
CREATE TABLE `ledger_account`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `account_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '账户编号',
  `owner_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '资金主体类型: PLATFORM/TENANT/MERCHANT/PSP/SYSTEM',
  `owner_id` bigint NOT NULL COMMENT '资金主体ID; 系统级账户可使用0',
  `account_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '账户类型: AVAILABLE/PENDING_SETTLE/FROZEN/CLEARING/FEE_INCOME等',
  `currency` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '币种: USD/CNY/BRL/INR等',
  `normal_side` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '账户余额方向: DEBIT/CREDIT',
  `allow_negative` tinyint NOT NULL DEFAULT 0 COMMENT '是否允许负余额: 0否 1是',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态: 1正常 2暂停 3停用',
  `remark` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '备注',
  `created_by` bigint NULL DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_by` bigint NULL DEFAULT NULL COMMENT '更新人ID',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_ledger_account_no`(`account_no` ASC) USING BTREE,
  UNIQUE INDEX `uk_ledger_account_owner`(`tenant_id` ASC, `owner_type` ASC, `owner_id` ASC, `account_type` ASC, `currency` ASC) USING BTREE,
  INDEX `idx_ledger_account_owner_currency`(`tenant_id` ASC, `owner_type` ASC, `owner_id` ASC, `currency` ASC) USING BTREE,
  INDEX `idx_ledger_account_type`(`tenant_id` ASC, `account_type` ASC, `currency` ASC, `status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2063922624787267586 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '账务账户' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for ledger_balance
-- ----------------------------
DROP TABLE IF EXISTS `ledger_balance`;
CREATE TABLE `ledger_balance`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `account_id` bigint NOT NULL COMMENT '账户ID',
  `account_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '账户编号',
  `currency` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '币种',
  `balance` decimal(28, 8) NOT NULL DEFAULT 0.00000000 COMMENT '当前余额',
  `debit_total` decimal(28, 8) NOT NULL DEFAULT 0.00000000 COMMENT '借方累计发生额',
  `credit_total` decimal(28, 8) NOT NULL DEFAULT 0.00000000 COMMENT '贷方累计发生额',
  `version` int NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `last_entry_id` bigint NULL DEFAULT NULL COMMENT '最后一条分录ID',
  `last_journal_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '最后一张凭证号',
  `last_posted_at` datetime(3) NULL DEFAULT NULL COMMENT '最后过账时间',
  `created_by` bigint NULL DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_by` bigint NULL DEFAULT NULL COMMENT '更新人ID',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_ledger_balance_account`(`tenant_id` ASC, `account_id` ASC) USING BTREE,
  UNIQUE INDEX `uk_ledger_balance_account_no`(`tenant_id` ASC, `account_no` ASC) USING BTREE,
  INDEX `idx_ledger_balance_currency`(`tenant_id` ASC, `currency` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2063922624787267586 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '账务余额缓存' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for ledger_entry
-- ----------------------------
DROP TABLE IF EXISTS `ledger_entry`;
CREATE TABLE `ledger_entry`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `journal_id` bigint NOT NULL COMMENT '凭证ID',
  `journal_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '凭证号',
  `entry_no` int NOT NULL COMMENT '凭证内分录序号',
  `account_id` bigint NOT NULL COMMENT '账户ID',
  `account_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '账户编号',
  `owner_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '资金主体类型快照',
  `owner_id` bigint NOT NULL COMMENT '资金主体ID快照',
  `account_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '账户类型快照',
  `currency` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '币种',
  `normal_side` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '账户余额方向快照: DEBIT/CREDIT',
  `direction` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '记账方向: DEBIT/CREDIT',
  `amount` decimal(24, 8) NOT NULL COMMENT '分录金额，永远为正数',
  `balance_change` decimal(24, 8) NOT NULL COMMENT '余额变动金额，可正可负',
  `balance_before` decimal(24, 8) NOT NULL COMMENT '变动前余额',
  `balance_after` decimal(24, 8) NOT NULL COMMENT '变动后余额',
  `biz_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '业务类型快照',
  `biz_id` bigint NULL DEFAULT NULL COMMENT '业务ID快照',
  `biz_no` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '业务编号快照',
  `event_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '业务事件快照',
  `summary` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '摘要',
  `remark` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '备注',
  `created_by` bigint NULL DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_by` bigint NULL DEFAULT NULL COMMENT '更新人ID',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_ledger_entry_journal_no`(`tenant_id` ASC, `journal_id` ASC, `entry_no` ASC) USING BTREE,
  INDEX `idx_ledger_entry_journal`(`tenant_id` ASC, `journal_no` ASC) USING BTREE,
  INDEX `idx_ledger_entry_account_created`(`tenant_id` ASC, `account_id` ASC, `created_at` ASC) USING BTREE,
  INDEX `idx_ledger_entry_owner_created`(`tenant_id` ASC, `owner_type` ASC, `owner_id` ASC, `currency` ASC, `created_at` ASC) USING BTREE,
  INDEX `idx_ledger_entry_account_type_created`(`tenant_id` ASC, `account_type` ASC, `currency` ASC, `created_at` ASC) USING BTREE,
  INDEX `idx_ledger_entry_biz`(`tenant_id` ASC, `biz_type` ASC, `biz_no` ASC, `event_type` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '账务账变流水/会计分录' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for ledger_hold
-- ----------------------------
DROP TABLE IF EXISTS `ledger_hold`;
CREATE TABLE `ledger_hold`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `hold_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '冻结编号',
  `owner_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '资金主体类型: MERCHANT/PSP等',
  `owner_id` bigint NOT NULL COMMENT '资金主体ID',
  `currency` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '币种',
  `available_account_id` bigint NOT NULL COMMENT '可用账户ID',
  `available_account_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '可用账户编号',
  `frozen_account_id` bigint NOT NULL COMMENT '冻结账户ID',
  `frozen_account_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '冻结账户编号',
  `biz_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '业务类型: PAYOUT_ORDER/RISK_CONTROL/SECURITY_DEPOSIT/MANUAL_FREEZE等',
  `biz_id` bigint NULL DEFAULT NULL COMMENT '业务ID',
  `biz_no` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '业务编号',
  `hold_reason` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '冻结原因',
  `hold_scope` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ORDER' COMMENT '冻结作用域: ORDER/RISK/DEPOSIT/MANUAL',
  `hold_amount` decimal(24, 8) NOT NULL DEFAULT 0.00000000 COMMENT '冻结总金额',
  `released_amount` decimal(24, 8) NOT NULL DEFAULT 0.00000000 COMMENT '已释放金额',
  `consumed_amount` decimal(24, 8) NOT NULL DEFAULT 0.00000000 COMMENT '已消耗金额',
  `remaining_amount` decimal(24, 8) NOT NULL DEFAULT 0.00000000 COMMENT '剩余冻结金额',
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'HOLDING' COMMENT '状态: HOLDING/PART_RELEASED/EXPIRED/RELEASED/CONSUMED/CANCELLED',
  `hold_journal_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '冻结凭证号',
  `last_release_journal_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '最后释放凭证号',
  `consume_journal_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '消耗凭证号',
  `expired_at` datetime(3) NULL DEFAULT NULL COMMENT '冻结过期时间',
  `reason` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '原因说明',
  `remark` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '备注',
  `created_by` bigint NULL DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_by` bigint NULL DEFAULT NULL COMMENT '更新人ID',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_ledger_hold_no`(`tenant_id` ASC, `hold_no` ASC) USING BTREE,
  UNIQUE INDEX `uk_ledger_hold_biz_scope`(`tenant_id` ASC, `biz_type` ASC, `biz_no` ASC, `hold_scope` ASC) USING BTREE,
  INDEX `idx_ledger_hold_scope`(`tenant_id` ASC, `hold_scope` ASC, `status` ASC, `created_at` ASC) USING BTREE,
  INDEX `idx_ledger_hold_owner_status`(`tenant_id` ASC, `owner_type` ASC, `owner_id` ASC, `currency` ASC, `status` ASC) USING BTREE,
  INDEX `idx_ledger_hold_expired`(`tenant_id` ASC, `status` ASC, `expired_at` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '账务冻结明细' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for ledger_journal
-- ----------------------------
DROP TABLE IF EXISTS `ledger_journal`;
CREATE TABLE `ledger_journal`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `journal_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '凭证号',
  `biz_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '业务类型: PAYIN_ORDER/PAYOUT_ORDER/SETTLE_BATCH/RECON_ADJUST等',
  `biz_id` bigint NULL DEFAULT NULL COMMENT '业务ID',
  `biz_no` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '业务编号',
  `event_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '业务事件: PAYIN_SUCCESS/PAYOUT_FREEZE/PAYOUT_SUCCESS/PAYOUT_FAILED等',
  `currency` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '币种',
  `total_amount` decimal(24, 8) NOT NULL COMMENT '凭证总金额',
  `entry_count` int NOT NULL DEFAULT 0 COMMENT '分录数量',
  `idempotency_key` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '幂等键',
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'POSTED' COMMENT '状态: POSTED/REVERSED',
  `reverse_of_journal_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '冲正来源凭证号; 当前凭证为冲正凭证时填写',
  `reversed_by_journal_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '冲正凭证号; 当前凭证被冲正后填写',
  `source_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'SYSTEM' COMMENT '记账来源: ORDER/SETTLE/RECON/MANUAL/SYSTEM',
  `trace_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '链路追踪ID',
  `posted_at` datetime(3) NULL DEFAULT NULL COMMENT '过账时间',
  `remark` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '备注',
  `created_by` bigint NULL DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_by` bigint NULL DEFAULT NULL COMMENT '更新人ID',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_ledger_journal_no`(`tenant_id` ASC, `journal_no` ASC) USING BTREE,
  UNIQUE INDEX `uk_ledger_journal_idempotency`(`tenant_id` ASC, `idempotency_key` ASC) USING BTREE,
  UNIQUE INDEX `uk_ledger_journal_biz_event`(`tenant_id` ASC, `biz_type` ASC, `biz_no` ASC, `event_type` ASC) USING BTREE,
  INDEX `idx_ledger_journal_biz`(`tenant_id` ASC, `biz_type` ASC, `biz_no` ASC) USING BTREE,
  INDEX `idx_ledger_journal_source`(`tenant_id` ASC, `source_type` ASC, `created_at` ASC) USING BTREE,
  INDEX `idx_ledger_journal_status_created`(`tenant_id` ASC, `status` ASC, `created_at` ASC) USING BTREE,
  INDEX `idx_ledger_journal_posted`(`tenant_id` ASC, `posted_at` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '账务记账凭证' ROW_FORMAT = Dynamic;
