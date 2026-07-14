
DROP TABLE IF EXISTS "sys_tenant_currency";
CREATE TABLE "sys_tenant_currency" (
  "id" bigint NOT NULL,
  "tenant_id" bigint NOT NULL,
  "currency" varchar(8) NOT NULL,
  "status" SMALLINT NOT NULL DEFAULT '1',
  "sort" int NOT NULL DEFAULT '100',
  "remark" varchar(500) DEFAULT NULL,
  "created_by" bigint DEFAULT NULL,
  "created_at" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  "updated_by" bigint DEFAULT NULL,
  "updated_at" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY ("id"),
);
CREATE UNIQUE INDEX "uk_tenant_currency" ON "sys_tenant_currency" ("tenant_id","currency");
CREATE INDEX "idx_tenant_status" ON "sys_tenant_currency" ("tenant_id","status");
CREATE INDEX "idx_currency" ON "sys_tenant_currency" ("currency");


