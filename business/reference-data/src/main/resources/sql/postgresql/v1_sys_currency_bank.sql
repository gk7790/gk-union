
DROP TABLE IF EXISTS "sys_currency";
CREATE TABLE "sys_currency" (
  "id" bigint NOT NULL,
  "currency" varchar(8) NOT NULL,
  "currency_name" varchar(128) NOT NULL,
  "currency_symbol" varchar(16) DEFAULT NULL,
  "numeric_code" varchar(8) DEFAULT NULL,
  "minor_unit" SMALLINT NOT NULL DEFAULT '2',
  "status" SMALLINT NOT NULL DEFAULT '1',
  "sort" int NOT NULL DEFAULT '100',
  "remark" varchar(500) DEFAULT NULL,
  "created_by" bigint DEFAULT NULL,
  "created_at" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  "updated_by" bigint DEFAULT NULL,
  "updated_at" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY ("id"),
);
CREATE UNIQUE INDEX "uk_currency" ON "sys_currency" ("currency");
CREATE INDEX "idx_status_sort" ON "sys_currency" ("status","sort");

DROP TABLE IF EXISTS "sys_bank";
CREATE TABLE "sys_bank" (
  "id" bigint NOT NULL,
  "country_code" varchar(8) NOT NULL,
  "currency" varchar(8) NOT NULL,
  "bank_code" varchar(64) NOT NULL,
  "bank_name" varchar(255) NOT NULL,
  "bank_short_name" varchar(128) DEFAULT NULL,
  "swift_code" varchar(32) DEFAULT NULL,
  "local_clearing_code" varchar(64) DEFAULT NULL,
  "status" SMALLINT NOT NULL DEFAULT '1',
  "sort" int NOT NULL DEFAULT '100',
  "remark" varchar(500) DEFAULT NULL,
  "created_by" bigint DEFAULT NULL,
  "created_at" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  "updated_by" bigint DEFAULT NULL,
  "updated_at" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY ("id"),
);
CREATE UNIQUE INDEX "uk_country_currency_bank_code" ON "sys_bank" ("bank_code","country_code","currency");
CREATE INDEX "idx_country_currency_status" ON "sys_bank" ("country_code","currency","status");

