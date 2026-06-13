SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `merchant_balance_adjust_order`;
CREATE TABLE `merchant_balance_adjust_order` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `merchant_id` bigint NOT NULL COMMENT '商户ID',
  `merchant_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '商户号快照',
  `adjust_order_no` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '余额调整单号',
  `adjust_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '调整类型: RECHARGE/DEDUCT/REVERSE/SUPPLEMENT',
  `source_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'MANUAL' COMMENT '来源: MANUAL/SYSTEM/API',
  `currency` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '币种',
  `amount` decimal(24, 8) NOT NULL COMMENT '调整金额，永远为正数',
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'CREATED' COMMENT '状态: CREATED/POSTED/FAILED',
  `reason` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '调整原因',
  `related_order_no` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '关联业务单号',
  `reverse_of_journal_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '冲正来源凭证号',
  `ledger_journal_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '账本凭证号',
  `trace_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '链路追踪ID',
  `posted_at` datetime(3) NULL DEFAULT NULL COMMENT '入账时间',
  `operator_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ADMIN' COMMENT '操作方类型: ADMIN/SYSTEM/API',
  `operator_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '操作方ID',
  `extra_json` json NULL COMMENT '扩展JSON',
  `version` int NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
  `remark` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '备注',
  `created_by` bigint NULL DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_by` bigint NULL DEFAULT NULL COMMENT '更新人ID',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_mba_order_no` (`tenant_id` ASC, `adjust_order_no` ASC) USING BTREE,
  INDEX `idx_mba_merchant_created` (`tenant_id` ASC, `merchant_id` ASC, `created_at` ASC) USING BTREE,
  INDEX `idx_mba_type_status` (`tenant_id` ASC, `adjust_type` ASC, `status` ASC, `created_at` ASC) USING BTREE,
  INDEX `idx_mba_journal` (`tenant_id` ASC, `ledger_journal_no` ASC) USING BTREE,
  INDEX `idx_mba_related_order` (`tenant_id` ASC, `related_order_no` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '商户余额调整单' ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
