INSERT INTO `sys_menu` (`id`, `pid`, `name`, `path`, `type`, `status`, `auth_code`, `component`, `meta`, `sort`, `created_at`, `updated_at`)
SELECT 1960000000000000001, 0, 'ledger-center', '/ledger', 1, 1, NULL, NULL,
       '{"title":"账务中心","icon":"lucide:wallet"}', 80, NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 1960000000000000001);

INSERT INTO `sys_menu` (`id`, `pid`, `name`, `path`, `type`, `status`, `auth_code`, `component`, `meta`, `sort`, `created_at`, `updated_at`)
SELECT 1960000000000000005, 1960000000000000001, 'tenant-merchant-balance-list', '/ledger/tenant-merchant-balance/list', 2, 1,
       'ledger:tenant-merchant-balance:page', '/ledger/tenant-merchant-balance/list',
       '{"title":"商户余额","icon":"lucide:coins"}', 9, NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 1960000000000000005);

INSERT INTO `sys_menu` (`id`, `pid`, `name`, `path`, `type`, `status`, `auth_code`, `component`, `meta`, `sort`, `created_at`, `updated_at`)
SELECT 1960000000000000006, 1960000000000000005, 'tenant-merchant-balance-page', NULL, 5, 1,
       'ledger:tenant-merchant-balance:page', NULL,
       '{"title":"查看"}', 1, NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 1960000000000000006);
