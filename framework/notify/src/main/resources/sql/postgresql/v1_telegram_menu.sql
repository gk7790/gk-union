-- Telegram 鏈哄櫒浜虹鐞嗚彍鍗曪紙骞冲彴绉熸埛鎵ц锛沬d 鍙寜椤圭洰瑙勮寖璋冩暣锛?
-- 缁勪欢璺緞瀵瑰簲鍓嶇 views锛?notify/tg/bot/list

INSERT INTO "sys_menu" ("id", "pid", "name", "path", "type", "status", "auth_code", "component", "meta", "sort", "created_at", "updated_at")
SELECT 1950000000000000001, 0, 'notify-center', '/notify', 1, 1, NULL, NULL,
       '{"title":"閫氱煡涓績","icon":"lucide:bell"}', 85, NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM "sys_menu" WHERE "id" = 1950000000000000001);

INSERT INTO "sys_menu" ("id", "pid", "name", "path", "type", "status", "auth_code", "component", "meta", "sort", "created_at", "updated_at")
SELECT 1950000000000000002, 1950000000000000001, 'notify-tg', '/notify/tg', 1, 1, NULL, NULL,
       '{"title":"Telegram","icon":"lucide:send"}', 1, NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM "sys_menu" WHERE "id" = 1950000000000000002);

INSERT INTO "sys_menu" ("id", "pid", "name", "path", "type", "status", "auth_code", "component", "meta", "sort", "created_at", "updated_at")
SELECT 1950000000000000003, 1950000000000000002, 'tg-bot-list', '/notify/tg/bot/list', 2, 1, 'tg:bot:page',
       '/notify/tg/bot/list', '{"title":"鏈哄櫒浜虹鐞?,"icon":"lucide:bot"}', 1, NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM "sys_menu" WHERE "id" = 1950000000000000003);

INSERT INTO "sys_menu" ("id", "pid", "name", "path", "type", "status", "auth_code", "component", "meta", "sort", "created_at", "updated_at")
SELECT 1950000000000000004, 1950000000000000003, 'tg-bot-page', NULL, 5, 1, 'tg:bot:page,tg:bot:info', NULL,
       '{"title":"鏌ョ湅"}', 1, NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM "sys_menu" WHERE "id" = 1950000000000000004);

INSERT INTO "sys_menu" ("id", "pid", "name", "path", "type", "status", "auth_code", "component", "meta", "sort", "created_at", "updated_at")
SELECT 1950000000000000005, 1950000000000000003, 'tg-bot-save', NULL, 5, 1, 'tg:bot:save', NULL,
       '{"title":"鏂板"}', 2, NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM "sys_menu" WHERE "id" = 1950000000000000005);

INSERT INTO "sys_menu" ("id", "pid", "name", "path", "type", "status", "auth_code", "component", "meta", "sort", "created_at", "updated_at")
SELECT 1950000000000000006, 1950000000000000003, 'tg-bot-update', NULL, 5, 1, 'tg:bot:update', NULL,
       '{"title":"淇敼"}', 3, NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM "sys_menu" WHERE "id" = 1950000000000000006);

INSERT INTO "sys_menu" ("id", "pid", "name", "path", "type", "status", "auth_code", "component", "meta", "sort", "created_at", "updated_at")
SELECT 1950000000000000007, 1950000000000000003, 'tg-bot-delete', NULL, 5, 1, 'tg:bot:delete', NULL,
       '{"title":"鍒犻櫎"}', 4, NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM "sys_menu" WHERE "id" = 1950000000000000007);

INSERT INTO "sys_menu" ("id", "pid", "name", "path", "type", "status", "auth_code", "component", "meta", "sort", "created_at", "updated_at")
SELECT 1950000000000000008, 1950000000000000002, 'tg-account-list', '/notify/tg/account/list', 2, 1, 'tg:account:page',
       '/notify/tg/account/list', '{"title":"缁戝畾璁板綍","icon":"lucide:link"}', 2, NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM "sys_menu" WHERE "id" = 1950000000000000008);

INSERT INTO "sys_menu" ("id", "pid", "name", "path", "type", "status", "auth_code", "component", "meta", "sort", "created_at", "updated_at")
SELECT 1950000000000000009, 1950000000000000008, 'tg-account-page', NULL, 5, 1, 'tg:account:page,tg:account:info', NULL,
       '{"title":"鏌ョ湅"}', 1, NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM "sys_menu" WHERE "id" = 1950000000000000009);

INSERT INTO "sys_menu" ("id", "pid", "name", "path", "type", "status", "auth_code", "component", "meta", "sort", "created_at", "updated_at")
SELECT 1950000000000000010, 1950000000000000008, 'tg-account-unbind', NULL, 5, 1, 'tg:account:update', NULL,
       '{"title":"瑙ｇ粦"}', 2, NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM "sys_menu" WHERE "id" = 1950000000000000010);
