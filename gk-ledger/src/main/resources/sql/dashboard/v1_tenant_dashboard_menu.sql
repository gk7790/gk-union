INSERT INTO `sys_menu` (`id`, `pid`, `name`, `path`, `type`, `status`, `auth_code`, `component`, `meta`, `sort`, `created_at`, `updated_at`)
SELECT 1970000000000000001, 0, 'tenant-dashboard', '/dashboard/tenant', 2, 1,
       'dashboard:tenant:view', '/dashboard/tenant/index',
       '{"title":"首页","icon":"lucide:layout-dashboard","affixTab":true}', 1, NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 1970000000000000001);

INSERT INTO `sys_menu` (`id`, `pid`, `name`, `path`, `type`, `status`, `auth_code`, `component`, `meta`, `sort`, `created_at`, `updated_at`)
SELECT 1970000000000000002, 1970000000000000001, 'tenant-dashboard-view', NULL, 5, 1,
       'dashboard:tenant:view', NULL,
       '{"title":"查看"}', 1, NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 1970000000000000002);
