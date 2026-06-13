-- ----------------------------
-- 商户异步通知发送定时任务(Quartz)
-- beanName = merchantNotifyTask, 每 10 秒排空一次待通知任务
-- 也可在后台"定时任务"页面手动新增/调整 cron, 无需改代码
-- ----------------------------
INSERT INTO `schedule_job`
(`id`, `schedule_group`, `bean_name`, `params`, `cron_expression`, `status`, `remark`, `created_at`)
VALUES
(2064290000000000001, 'payment', 'merchantNotifyTask', NULL, '0/10 * * * * ?', 1, '商户异步通知发送/重试', NOW());
