-- GK Ledger 鑿滃崟涓庡畾鏃朵换鍔＄瀛愭暟鎹€?
-- 缁撴瀯琛ㄨ鍏堟墽琛屽悇妯″潡 schema锛屽啀鎵ц鏈枃浠躲€?

INSERT INTO "sys_menu" ("id", "pid", "name", "path", "type", "status", "auth_code", "component", "meta", "sort", "created_at", "updated_at")
SELECT 1970000000000000001, 0, 'tenant-dashboard', '/dashboard/tenant', 2, 1,
       'dashboard:tenant:view', '/dashboard/tenant/index',
       '{"title":"棣栭〉","icon":"lucide:layout-dashboard","affixTab":true}', 1, NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM "sys_menu" WHERE "id" = 1970000000000000001);

INSERT INTO "sys_menu" ("id", "pid", "name", "path", "type", "status", "auth_code", "component", "meta", "sort", "created_at", "updated_at")
SELECT 1970000000000000002, 1970000000000000001, 'tenant-dashboard-view', NULL, 5, 1,
       'dashboard:tenant:view', NULL,
       '{"title":"鏌ョ湅"}', 1, NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM "sys_menu" WHERE "id" = 1970000000000000002);

INSERT INTO "sys_menu" ("id", "pid", "name", "path", "type", "status", "auth_code", "component", "meta", "sort", "created_at", "updated_at")
SELECT 1970000000000000101, 0, 'merchant-dashboard', '/dashboard/merchant', 2, 1,
       'dashboard:merchant:view', '/dashboard/merchant/index',
       '{"title":"棣栭〉","icon":"lucide:layout-dashboard","affixTab":true}', 1, NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM "sys_menu" WHERE "id" = 1970000000000000101);

INSERT INTO "sys_menu" ("id", "pid", "name", "path", "type", "status", "auth_code", "component", "meta", "sort", "created_at", "updated_at")
SELECT 1970000000000000102, 1970000000000000101, 'merchant-dashboard-view', NULL, 5, 1,
       'dashboard:merchant:view', NULL,
       '{"title":"鏌ョ湅"}', 1, NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM "sys_menu" WHERE "id" = 1970000000000000102);

INSERT INTO "sys_menu" ("id", "pid", "name", "path", "type", "status", "auth_code", "component", "meta", "sort", "created_at", "updated_at")
SELECT 1960000000000000001, 0, 'ledger-center', '/ledger', 1, 1, NULL, NULL,
       '{"title":"璐㈠姟涓績","icon":"lucide:wallet"}', 80, NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM "sys_menu" WHERE "id" = 1960000000000000001);

INSERT INTO "sys_menu" ("id", "pid", "name", "path", "type", "status", "auth_code", "component", "meta", "sort", "created_at", "updated_at")
SELECT 1960000000000000005, 1960000000000000001, 'merchant-balance-list', '/ledger/merchant-balance/list', 2, 1,
       'ledger:merchant-balance:page', '/ledger/merchant-balance/list',
       '{"title":"鍟嗘埛浣欓","icon":"lucide:coins"}', 9, NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM "sys_menu" WHERE "id" = 1960000000000000005);

INSERT INTO "sys_menu" ("id", "pid", "name", "path", "type", "status", "auth_code", "component", "meta", "sort", "created_at", "updated_at")
SELECT 1960000000000000006, 1960000000000000005, 'merchant-balance-page', NULL, 5, 1,
       'ledger:merchant-balance:page', NULL,
       '{"title":"鏌ョ湅"}', 1, NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM "sys_menu" WHERE "id" = 1960000000000000006);

INSERT INTO "sys_menu" ("id", "pid", "name", "path", "type", "status", "auth_code", "component", "meta", "sort", "created_at", "updated_at")
SELECT 1960000000000000002, 1960000000000000001, 'merchant-balance-adjust-list', '/ledger/merchant-balance-adjust/list', 2, 1,
       'ledger:merchant-balance-adjust:page', '/ledger/merchant-balance-adjust/list',
       '{"title":"鍟嗘埛浣欓璋冩暣","icon":"lucide:landmark"}', 10, NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM "sys_menu" WHERE "id" = 1960000000000000002);

INSERT INTO "sys_menu" ("id", "pid", "name", "path", "type", "status", "auth_code", "component", "meta", "sort", "created_at", "updated_at")
SELECT 1960000000000000003, 1960000000000000002, 'merchant-balance-adjust-page', NULL, 5, 1,
       'ledger:merchant-balance-adjust:page,ledger:merchant-balance-adjust:info', NULL,
       '{"title":"鏌ョ湅"}', 1, NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM "sys_menu" WHERE "id" = 1960000000000000003);

INSERT INTO "sys_menu" ("id", "pid", "name", "path", "type", "status", "auth_code", "component", "meta", "sort", "created_at", "updated_at")
SELECT 1960000000000000004, 1960000000000000002, 'merchant-balance-adjust-submit', NULL, 5, 1,
       'ledger:merchant-balance-adjust:submit', NULL,
       '{"title":"鎻愪氦璋冩暣"}', 2, NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM "sys_menu" WHERE "id" = 1960000000000000004);

INSERT INTO "sys_menu" ("id", "pid", "name", "path", "type", "status", "auth_code", "component", "meta", "sort", "created_at", "updated_at")
SELECT 1950000000000000001, 0, 'payment-config', '/payment/config', 1, 1, NULL, NULL,
       '{"title":"鏀粯閰嶇疆","icon":"lucide:settings"}', 70, NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM "sys_menu" WHERE "id" = 1950000000000000001);

INSERT INTO "sys_menu" ("id", "pid", "name", "path", "type", "status", "auth_code", "component", "meta", "sort", "created_at", "updated_at")
SELECT 1950000000000000100, 1950000000000000001, 'payment-plan-list', '/payment/payment-plan/list', 2, 1,
       'payment:payment-plan:page', '/payment/payment-plan/list',
       '{"title":"鏀粯鏂规","icon":"lucide:route"}', 40, NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM "sys_menu" WHERE "id" = 1950000000000000100);

INSERT INTO "sys_menu" ("id", "pid", "name", "path", "type", "status", "auth_code", "component", "meta", "sort", "created_at", "updated_at")
SELECT 1950000000000000101, 1950000000000000100, 'payment-plan-page', NULL, 5, 1,
       'payment:payment-plan:page,payment:payment-plan:info', NULL,
       '{"title":"鏌ョ湅"}', 1, NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM "sys_menu" WHERE "id" = 1950000000000000101);

INSERT INTO "sys_menu" ("id", "pid", "name", "path", "type", "status", "auth_code", "component", "meta", "sort", "created_at", "updated_at")
SELECT 1950000000000000102, 1950000000000000100, 'payment-plan-preview', NULL, 5, 1,
       'payment:payment-plan:preview', NULL,
       '{"title":"棰勮"}', 2, NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM "sys_menu" WHERE "id" = 1950000000000000102);

INSERT INTO "sys_menu" ("id", "pid", "name", "path", "type", "status", "auth_code", "component", "meta", "sort", "created_at", "updated_at")
SELECT 1950000000000000103, 1950000000000000100, 'payment-plan-publish', NULL, 5, 1,
       'payment:payment-plan:publish', NULL,
       '{"title":"鍙戝竷"}', 3, NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM "sys_menu" WHERE "id" = 1950000000000000103);

INSERT INTO "sys_menu" ("id", "pid", "name", "path", "type", "status", "auth_code", "component", "meta", "sort", "created_at", "updated_at")
SELECT 1950000000000000104, 1950000000000000100, 'payment-plan-activate', NULL, 5, 1,
       'payment:payment-plan:activate', NULL,
       '{"title":"婵€娲?}', 4, NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM "sys_menu" WHERE "id" = 1950000000000000104);

INSERT INTO "sys_menu" ("id", "pid", "name", "path", "type", "status", "auth_code", "component", "meta", "sort", "created_at", "updated_at")
SELECT 1950000000000000105, 1950000000000000100, 'payment-plan-retire', NULL, 5, 1,
       'payment:payment-plan:retire', NULL,
       '{"title":"鍋滅敤"}', 5, NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM "sys_menu" WHERE "id" = 1950000000000000105);

INSERT INTO "sys_menu" ("id", "pid", "name", "path", "type", "status", "auth_code", "component", "meta", "sort", "created_at", "updated_at")
SELECT 1950000000000000201, 1950000000000000001, 'payment-route-channel-options', NULL, 5, 1,
       'payment:route-channel:options', NULL,
       '{"title":"璺敱閫氶亾绾ц仈閫夐」"}', 50, NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM "sys_menu" WHERE "id" = 1950000000000000201);

INSERT INTO "sys_menu" ("id", "pid", "name", "path", "type", "status", "auth_code", "component", "meta", "sort", "created_at", "updated_at")
SELECT 1950000000000000202, 1950000000000000001, 'payment-route-group-check', NULL, 5, 1,
       'payment:route-group:check', NULL,
       '{"title":"妫€娴嬭矾鐢辩粍"}', 51, NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM "sys_menu" WHERE "id" = 1950000000000000202);

INSERT INTO "schedule_job"
("id", "schedule_group", "bean_name", "params", "cron_expression", "status", "remark", "created_at")
SELECT 2064290000000000001, 'payment', 'merchantNotifyTask', NULL, '0/10 * * * * ?', 1, '鍟嗘埛寮傛閫氱煡鍙戦€?閲嶈瘯', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM "schedule_job" WHERE "id" = 2064290000000000001);

INSERT INTO "schedule_job"
("id", "schedule_group", "bean_name", "params", "cron_expression", "status", "remark", "created_at")
SELECT 2064290000000000002, 'payment', 'payinOrderQueryTask', NULL, '0/30 * * * * ?', 1, '浠ｆ敹璁㈠崟涓诲姩鏌ュ崟琛ュ伩', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM "schedule_job" WHERE "id" = 2064290000000000002);

INSERT INTO "schedule_job"
("id", "schedule_group", "bean_name", "params", "cron_expression", "status", "remark", "created_at")
SELECT 2064290000000000003, 'payment', 'payoutOrderQueryTask', NULL, '0/30 * * * * ?', 1, '浠ｄ粯璁㈠崟涓诲姩鏌ュ崟琛ュ伩', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM "schedule_job" WHERE "id" = 2064290000000000003);

INSERT INTO "schedule_job"
("id", "schedule_group", "bean_name", "params", "cron_expression", "status", "remark", "created_at")
SELECT 2064290000000000004, 'payment', 'paySettleReleaseTask', NULL, '0 0/1 * * * ?', 1, '浠ｆ敹寰呯粨绠楄嚜鍔ㄩ噴鏀捐嚦鍟嗘埛鍙敤浣欓', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM "schedule_job" WHERE "id" = 2064290000000000004);

INSERT INTO "schedule_job"
("id", "schedule_group", "bean_name", "params", "cron_expression", "status", "remark", "created_at")
SELECT 2064290000000000005, 'payment', 'payinOrderCloseTask', NULL, '0/30 * * * * ?', 1, '浠ｆ敹瓒呮椂鏈敮浠樺叧鍗?, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM "schedule_job" WHERE "id" = 2064290000000000005);

INSERT INTO "schedule_job"
("id", "schedule_group", "bean_name", "params", "cron_expression", "status", "remark", "created_at")
SELECT 2064290000000000006, 'payment', 'payoutOrderExceptionTask', NULL, '0 0/5 * * * ?', 1, '浠ｄ粯闀挎椂闂村鐞嗕腑杞汉宸ュ鐞?, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM "schedule_job" WHERE "id" = 2064290000000000006);

INSERT INTO "schedule_job"
("id", "schedule_group", "bean_name", "params", "cron_expression", "status", "remark", "created_at")
SELECT 2064290000000000007, 'ledger', 'ledgerHoldExpireTask', NULL, '0 0/5 * * * ?', 1, '璐﹀姟鍐荤粨杩囨湡鎵弿', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM "schedule_job" WHERE "id" = 2064290000000000007);

INSERT INTO "schedule_job"
("id", "schedule_group", "bean_name", "params", "cron_expression", "status", "remark", "created_at")
SELECT 2064290000000000008, 'payment', 'payinOrderExceptionTask', NULL, '0 0/5 * * * ?', 1, '浠ｆ敹闀挎椂闂村鐞嗕腑杞汉宸ュ鐞?, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM "schedule_job" WHERE "id" = 2064290000000000008);

INSERT INTO "schedule_job"
("id", "schedule_group", "bean_name", "params", "cron_expression", "status", "remark", "created_at")
SELECT 2064290000000000009, 'payment', 'payoutSubmitOutboxTask', NULL, '0/10 * * * * ?', 1, '浠ｄ粯鎻愪氦 PSP outbox 娑堣垂', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM "schedule_job" WHERE "id" = 2064290000000000009);

INSERT INTO "schedule_job"
("id", "schedule_group", "bean_name", "params", "cron_expression", "status", "remark", "created_at")
SELECT 2064290000000000010, 'psp', 'pspBalanceTask', NULL, '0 0/1 * * * ?', 1, 'PSP balance snap refresh', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM "schedule_job" WHERE "id" = 2064290000000000010);
