ALTER TABLE `merchant`
    ADD COLUMN `tg_user_id` bigint NULL DEFAULT NULL COMMENT 'Telegram用户ID' AFTER `contact_phone`;
