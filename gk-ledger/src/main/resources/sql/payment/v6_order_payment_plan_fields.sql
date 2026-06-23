-- 订单表补充支付方案命中字段。
-- 适用于已经存在 pay_order 和 payout_order 的数据库。

ALTER TABLE `pay_order`
  ADD COLUMN `payment_plan_catalog_id` bigint NULL DEFAULT NULL COMMENT '命中的支付方案目录ID' AFTER `failed_at`,
  ADD COLUMN `payment_plan_version` bigint NULL DEFAULT NULL COMMENT '命中的支付方案版本号' AFTER `payment_plan_catalog_id`,
  ADD COLUMN `payment_plan_bucket_id` bigint NULL DEFAULT NULL COMMENT '命中的支付方案金额段ID' AFTER `payment_plan_version`,
  ADD COLUMN `payment_plan_route_option_id` bigint NULL DEFAULT NULL COMMENT '命中的支付方案路由候选ID' AFTER `payment_plan_bucket_id`,
  ADD COLUMN `route_group_id` bigint NULL DEFAULT NULL COMMENT '命中的支付路由组ID' AFTER `route_rule_id`,
  ADD COLUMN `route_channel_id` bigint NULL DEFAULT NULL COMMENT '命中的支付路由通道ID' AFTER `route_group_id`,
  ADD INDEX `idx_pay_order_payment_plan` (`tenant_id`, `payment_plan_catalog_id`, `payment_plan_version`, `created_at`) USING BTREE,
  ADD INDEX `idx_pay_order_route_source` (`tenant_id`, `route_group_id`, `route_channel_id`, `created_at`) USING BTREE;

ALTER TABLE `payout_order`
  ADD COLUMN `payment_plan_catalog_id` bigint NULL DEFAULT NULL COMMENT '命中的支付方案目录ID' AFTER `release_journal_no`,
  ADD COLUMN `payment_plan_version` bigint NULL DEFAULT NULL COMMENT '命中的支付方案版本号' AFTER `payment_plan_catalog_id`,
  ADD COLUMN `payment_plan_bucket_id` bigint NULL DEFAULT NULL COMMENT '命中的支付方案金额段ID' AFTER `payment_plan_version`,
  ADD COLUMN `payment_plan_route_option_id` bigint NULL DEFAULT NULL COMMENT '命中的支付方案路由候选ID' AFTER `payment_plan_bucket_id`,
  ADD COLUMN `route_group_id` bigint NULL DEFAULT NULL COMMENT '命中的支付路由组ID' AFTER `route_rule_id`,
  ADD COLUMN `route_channel_id` bigint NULL DEFAULT NULL COMMENT '命中的支付路由通道ID' AFTER `route_group_id`,
  ADD INDEX `idx_payout_order_payment_plan` (`tenant_id`, `payment_plan_catalog_id`, `payment_plan_version`, `created_at`) USING BTREE,
  ADD INDEX `idx_payout_order_route_source` (`tenant_id`, `route_group_id`, `route_channel_id`, `created_at`) USING BTREE;
