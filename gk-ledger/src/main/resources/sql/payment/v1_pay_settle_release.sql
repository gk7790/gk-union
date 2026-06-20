-- 代收待结算 → 可用释放（方案 B）
ALTER TABLE `pay_order`
    ADD COLUMN `settle_release_at` datetime(3) NULL DEFAULT NULL COMMENT '计划结算释放时间' AFTER `settle_status`;

CREATE INDEX `idx_pay_order_settle_release` ON `pay_order` (`tenant_id`, `settle_status`, `settle_release_at`);

INSERT INTO `schedule_job`
(`id`, `schedule_group`, `bean_name`, `params`, `cron_expression`, `status`, `remark`, `created_at`)
VALUES
(2064290000000000004, 'payment', 'paySettleReleaseTask', NULL, '0 0/1 * * * ?', 1, '代收待结算自动释放至商户可用余额', NOW());

-- 存量商户：按需为已有商户/币种补开 PENDING_SETTLE 账户（首次入账时会懒创建，此处可选）
-- 存量 SUCCESS 且资金已在 AVAILABLE 的订单需运营侧单独迁移，本脚本不自动搬余额。
