-- 菜单为平台全局目录，租户维度由 sys_role.tenant_id + sys_role_menu 表达，不再使用 sys_menu.tenant_id

ALTER TABLE `sys_menu` DROP COLUMN `tenant_id`;
