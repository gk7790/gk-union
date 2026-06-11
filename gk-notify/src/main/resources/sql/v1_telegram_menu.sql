-- Telegram 机器人管理菜单（平台租户执行；id 可按项目规范调整）
-- 组件路径对应前端 views：/notify/tg/bot/list

INSERT INTO `sys_menu` (`id`, `tenant_id`, `pid`, `name`, `path`, `type`, `status`, `auth_code`, `component`, `meta`, `sort`, `created_at`, `updated_at`)
SELECT 1950000000000000001, 0, 0, 'notify-center', '/notify', 1, 1, NULL, NULL,
       '{"title":"通知中心","icon":"lucide:bell"}', 85, NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 1950000000000000001);

INSERT INTO `sys_menu` (`id`, `tenant_id`, `pid`, `name`, `path`, `type`, `status`, `auth_code`, `component`, `meta`, `sort`, `created_at`, `updated_at`)
SELECT 1950000000000000002, 0, 1950000000000000001, 'notify-tg', '/notify/tg', 1, 1, NULL, NULL,
       '{"title":"Telegram","icon":"lucide:send"}', 1, NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 1950000000000000002);

INSERT INTO `sys_menu` (`id`, `tenant_id`, `pid`, `name`, `path`, `type`, `status`, `auth_code`, `component`, `meta`, `sort`, `created_at`, `updated_at`)
SELECT 1950000000000000003, 0, 1950000000000000002, 'tg-bot-list', '/notify/tg/bot/list', 2, 1, 'tg:bot:page',
       '/notify/tg/bot/list', '{"title":"机器人管理","icon":"lucide:bot"}', 1, NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 1950000000000000003);

INSERT INTO `sys_menu` (`id`, `tenant_id`, `pid`, `name`, `path`, `type`, `status`, `auth_code`, `component`, `meta`, `sort`, `created_at`, `updated_at`)
SELECT 1950000000000000004, 0, 1950000000000000003, 'tg-bot-page', NULL, 5, 1, 'tg:bot:page,tg:bot:info', NULL,
       '{"title":"查看"}', 1, NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 1950000000000000004);

INSERT INTO `sys_menu` (`id`, `tenant_id`, `pid`, `name`, `path`, `type`, `status`, `auth_code`, `component`, `meta`, `sort`, `created_at`, `updated_at`)
SELECT 1950000000000000005, 0, 1950000000000000003, 'tg-bot-save', NULL, 5, 1, 'tg:bot:save', NULL,
       '{"title":"新增"}', 2, NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 1950000000000000005);

INSERT INTO `sys_menu` (`id`, `tenant_id`, `pid`, `name`, `path`, `type`, `status`, `auth_code`, `component`, `meta`, `sort`, `created_at`, `updated_at`)
SELECT 1950000000000000006, 0, 1950000000000000003, 'tg-bot-update', NULL, 5, 1, 'tg:bot:update', NULL,
       '{"title":"修改"}', 3, NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 1950000000000000006);

INSERT INTO `sys_menu` (`id`, `tenant_id`, `pid`, `name`, `path`, `type`, `status`, `auth_code`, `component`, `meta`, `sort`, `created_at`, `updated_at`)
SELECT 1950000000000000007, 0, 1950000000000000003, 'tg-bot-delete', NULL, 5, 1, 'tg:bot:delete', NULL,
       '{"title":"删除"}', 4, NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 1950000000000000007);

INSERT INTO `sys_menu` (`id`, `tenant_id`, `pid`, `name`, `path`, `type`, `status`, `auth_code`, `component`, `meta`, `sort`, `created_at`, `updated_at`)
SELECT 1950000000000000008, 0, 1950000000000000002, 'tg-account-list', '/notify/tg/account/list', 2, 1, 'tg:account:page',
       '/notify/tg/account/list', '{"title":"绑定记录","icon":"lucide:link"}', 2, NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 1950000000000000008);

INSERT INTO `sys_menu` (`id`, `tenant_id`, `pid`, `name`, `path`, `type`, `status`, `auth_code`, `component`, `meta`, `sort`, `created_at`, `updated_at`)
SELECT 1950000000000000009, 0, 1950000000000000008, 'tg-account-page', NULL, 5, 1, 'tg:account:page,tg:account:info', NULL,
       '{"title":"查看"}', 1, NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 1950000000000000009);

INSERT INTO `sys_menu` (`id`, `tenant_id`, `pid`, `name`, `path`, `type`, `status`, `auth_code`, `component`, `meta`, `sort`, `created_at`, `updated_at`)
SELECT 1950000000000000010, 0, 1950000000000000008, 'tg-account-unbind', NULL, 5, 1, 'tg:account:update', NULL,
       '{"title":"解绑"}', 2, NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 1950000000000000010);

INSERT INTO `sys_menu` (`id`, `tenant_id`, `pid`, `name`, `path`, `type`, `status`, `auth_code`, `component`, `meta`, `sort`, `created_at`, `updated_at`)
SELECT 1950000000000000011, 0, 1950000000000000002, 'tg-bind-index', '/notify/tg/bind/index', 2, 1, 'tg:bind-code:generate',
       '/notify/tg/bind/index', '{"title":"账号绑定","icon":"lucide:user-check"}', 3, NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 1950000000000000011);

INSERT INTO `sys_menu` (`id`, `tenant_id`, `pid`, `name`, `path`, `type`, `status`, `auth_code`, `component`, `meta`, `sort`, `created_at`, `updated_at`)
SELECT 1950000000000000012, 0, 1950000000000000011, 'tg-bind-generate', NULL, 5, 1, 'tg:bind-code:generate', NULL,
       '{"title":"生成绑定码"}', 1, NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 1950000000000000012);
