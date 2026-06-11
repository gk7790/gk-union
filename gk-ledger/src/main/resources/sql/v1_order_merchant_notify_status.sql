-- 订单下游商户通知状态
ALTER TABLE `pay_order`
    ADD COLUMN `merchant_notify_status` varchar(16) NULL DEFAULT NULL COMMENT '下游商户通知状态: NONE/PENDING/SUCCESS/FAILED' AFTER `return_url`,
    ADD COLUMN `merchant_notify_at` datetime(3) NULL DEFAULT NULL COMMENT '下游商户通知完成/最近尝试时间' AFTER `merchant_notify_status`,
    ADD COLUMN `merchant_notify_task_id` bigint NULL DEFAULT NULL COMMENT '关联商户通知任务ID' AFTER `merchant_notify_at`,
    ADD INDEX `idx_pay_order_merchant_notify`(`tenant_id` ASC, `merchant_notify_status` ASC, `created_at` ASC) USING BTREE;

ALTER TABLE `payout_order`
    ADD COLUMN `merchant_notify_status` varchar(16) NULL DEFAULT NULL COMMENT '下游商户通知状态: NONE/PENDING/SUCCESS/FAILED' AFTER `notify_url`,
    ADD COLUMN `merchant_notify_at` datetime(3) NULL DEFAULT NULL COMMENT '下游商户通知完成/最近尝试时间' AFTER `merchant_notify_status`,
    ADD COLUMN `merchant_notify_task_id` bigint NULL DEFAULT NULL COMMENT '关联商户通知任务ID' AFTER `merchant_notify_at`,
    ADD INDEX `idx_payout_order_merchant_notify`(`tenant_id` ASC, `merchant_notify_status` ASC, `created_at` ASC) USING BTREE;

UPDATE `pay_order`
SET `merchant_notify_status` = 'NONE'
WHERE (`notify_url` IS NULL OR `notify_url` = '')
  AND `merchant_notify_status` IS NULL;

UPDATE `payout_order`
SET `merchant_notify_status` = 'NONE'
WHERE (`notify_url` IS NULL OR `notify_url` = '')
  AND `merchant_notify_status` IS NULL;
