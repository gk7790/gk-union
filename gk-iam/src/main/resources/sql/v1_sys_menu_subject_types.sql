-- sys_menu.scope (integer json) -> subject_types (string json: PLATFORM/TENANT/MERCHANT)
-- 存量数据默认对三类主体可见，可在菜单管理中按业务再收紧。

ALTER TABLE `sys_menu`
    ADD COLUMN `subject_types` json NULL COMMENT '可见主体: PLATFORM/TENANT/MERCHANT' AFTER `sort`;

UPDATE `sys_menu`
SET `subject_types` = '["PLATFORM","TENANT","MERCHANT"]'
WHERE `subject_types` IS NULL;

ALTER TABLE `sys_menu` DROP COLUMN `scope`;
