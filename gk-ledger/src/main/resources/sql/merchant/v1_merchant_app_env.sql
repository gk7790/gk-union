ALTER TABLE `merchant_app`
    ADD COLUMN `app_env` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PROD'
        COMMENT '应用环境: TEST/PROD' AFTER `app_type`,
    ADD INDEX `idx_merchant_app_env`(`tenant_id` ASC, `merchant_id` ASC, `app_env` ASC) USING BTREE;

ALTER TABLE `merchant_app`
    MODIFY COLUMN `app_env` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'TEST'
        COMMENT '应用环境: TEST/PROD';
