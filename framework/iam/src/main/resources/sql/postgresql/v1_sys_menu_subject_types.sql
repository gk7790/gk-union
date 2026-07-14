-- sys_menu.scope (integer json) -> subject_types (string json: PLATFORM/TENANT/MERCHANT)
-- 瀛橀噺鏁版嵁榛樿瀵逛笁绫讳富浣撳彲瑙侊紝鍙湪鑿滃崟绠＄悊涓寜涓氬姟鍐嶆敹绱с€?

ALTER TABLE "sys_menu"
    ADD COLUMN "subject_types" json NULL

UPDATE "sys_menu"
SET "subject_types" = '["PLATFORM","TENANT","MERCHANT"]'
WHERE "subject_types" IS NULL;

ALTER TABLE "sys_menu" DROP COLUMN "scope";
