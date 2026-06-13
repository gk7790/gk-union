INSERT INTO `sys_menu` (`id`, `pid`, `name`, `path`, `type`, `status`, `auth_code`, `component`, `meta`, `sort`, `created_at`, `updated_at`)
SELECT 1960000000000000001, 0, 'ledger-center', '/ledger', 1, 1, NULL, NULL,
       '{"title":"账务中心","icon":"lucide:wallet"}', 80, NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 1960000000000000001);

INSERT INTO `sys_menu` (`id`, `pid`, `name`, `path`, `type`, `status`, `auth_code`, `component`, `meta`, `sort`, `created_at`, `updated_at`)
SELECT 1960000000000000002, 1960000000000000001, 'merchant-balance-adjust-list', '/ledger/merchant-balance-adjust/list', 2, 1,
       'ledger:merchant-balance-adjust:page', '/ledger/merchant-balance-adjust/list',
       '{"title":"商户余额调整","icon":"lucide:landmark"}', 10, NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 1960000000000000002);

INSERT INTO `sys_menu` (`id`, `pid`, `name`, `path`, `type`, `status`, `auth_code`, `component`, `meta`, `sort`, `created_at`, `updated_at`)
SELECT 1960000000000000003, 1960000000000000002, 'merchant-balance-adjust-page', NULL, 5, 1,
       'ledger:merchant-balance-adjust:page,ledger:merchant-balance-adjust:info', NULL,
       '{"title":"查看"}', 1, NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 1960000000000000003);

INSERT INTO `sys_menu` (`id`, `pid`, `name`, `path`, `type`, `status`, `auth_code`, `component`, `meta`, `sort`, `created_at`, `updated_at`)
SELECT 1960000000000000004, 1960000000000000002, 'merchant-balance-adjust-submit', NULL, 5, 1,
       'ledger:merchant-balance-adjust:submit', NULL,
       '{"title":"提交调整"}', 2, NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 1960000000000000004);
