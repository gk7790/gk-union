ALTER TABLE `psp_fee_rule`
    ADD COLUMN `psp_method_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'PSP支付方式编码快照' AFTER `psp_method_id`;

UPDATE `psp_fee_rule` r
    LEFT JOIN `psp_method` m ON m.`id` = r.`psp_method_id`
SET r.`psp_method_code` = m.`psp_method_code`
WHERE r.`psp_method_id` IS NOT NULL;
