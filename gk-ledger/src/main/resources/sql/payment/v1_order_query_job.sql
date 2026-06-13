-- ----------------------------
-- PSP active order query compensation fields and Quartz jobs
-- ----------------------------

ALTER TABLE `pay_order`
    ADD COLUMN `next_query_at` datetime(3) NULL DEFAULT NULL COMMENT 'next PSP status query time' AFTER `psp_pay_params_json`,
    ADD COLUMN `query_count` int NOT NULL DEFAULT 0 COMMENT 'PSP status query count' AFTER `next_query_at`;

ALTER TABLE `pay_order`
    ADD INDEX `idx_pay_order_query`(`tenant_id` ASC, `status` ASC, `next_query_at` ASC) USING BTREE;

INSERT INTO `schedule_job`
(`id`, `schedule_group`, `bean_name`, `params`, `cron_expression`, `status`, `remark`, `created_at`)
VALUES
(2064290000000000002, 'payment', 'payOrderQueryTask', NULL, '0/30 * * * * ?', 1, 'pay order active PSP query compensation', NOW()),
(2064290000000000003, 'payment', 'payoutOrderQueryTask', NULL, '0/30 * * * * ?', 1, 'payout order active PSP query compensation', NOW());
