-- ----------------------------
-- Telegram message send Quartz jobs
-- Same beanName = tgMessageTask, split by params/biz_type.
-- SYSTEM_ALERT handles: SYSTEM_ERROR, SYSTEM_WARN, RISK_WARN.
-- BUSINESS_NOTIFY handles: PAYIN_NOTICE, PAYOUT_NOTICE, CHANNEL_NOTICE, MERCHANT_NOTICE, ORDER_NOTICE, PSP_NOTICE.
-- ----------------------------

INSERT INTO `schedule_job`
(`id`, `schedule_group`, `bean_name`, `params`, `cron_expression`, `status`, `remark`, `created_at`)
VALUES
(2064290000000000008, 'telegram', 'tgMessageTask', 'SYSTEM_ALERT', '0/5 * * * * ?', 1, 'Telegram系统告警发送/重试', NOW()),
(2064290000000000009, 'telegram', 'tgMessageTask', 'BUSINESS_NOTIFY', '0/10 * * * * ?', 1, 'Telegram业务通知发送/重试', NOW());
