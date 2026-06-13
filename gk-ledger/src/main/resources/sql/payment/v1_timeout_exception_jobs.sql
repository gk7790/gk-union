-- ----------------------------
-- Payment timeout and exception compensation Quartz jobs
-- ----------------------------

INSERT INTO `schedule_job`
(`id`, `schedule_group`, `bean_name`, `params`, `cron_expression`, `status`, `remark`, `created_at`)
VALUES
(2064290000000000005, 'payment', 'payOrderCloseTask', NULL, '0/30 * * * * ?', 1, '代收超时未支付关单', NOW()),
(2064290000000000006, 'payment', 'payoutOrderExceptionTask', NULL, '0 0/5 * * * ?', 1, '代付长时间处理中转人工处理', NOW()),
(2064290000000000007, 'ledger', 'ledgerHoldExpireTask', NULL, '0 0/5 * * * ?', 1, '账务冻结过期扫描', NOW());
