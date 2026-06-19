INSERT INTO `sys_menu` (`id`, `pid`, `name`, `path`, `type`, `status`, `auth_code`, `component`, `meta`, `sort`, `created_at`, `updated_at`)
SELECT 1970000000000000101, 0, 'merchant-dashboard', '/dashboard/merchant', 2, 1,
       'dashboard:merchant:view', '/dashboard/merchant/index',
       '{"title":"首页","icon":"lucide:layout-dashboard","affixTab":true}', 1, NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 1970000000000000101);

INSERT INTO `sys_menu` (`id`, `pid`, `name`, `path`, `type`, `status`, `auth_code`, `component`, `meta`, `sort`, `created_at`, `updated_at`)
SELECT 1970000000000000102, 1970000000000000101, 'merchant-dashboard-view', NULL, 5, 1,
       'dashboard:merchant:view', NULL,
       '{"title":"查看"}', 1, NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 1970000000000000102);
