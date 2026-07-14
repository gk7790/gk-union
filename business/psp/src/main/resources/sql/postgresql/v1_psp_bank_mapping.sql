
DROP TABLE IF EXISTS "psp_bank_mapping";
CREATE TABLE "psp_bank_mapping" (
  "id" bigint NOT NULL,
  "psp_id" bigint NOT NULL,
  "country_code" varchar(8) NOT NULL,
  "currency" varchar(8) NOT NULL,
  "bank_code" varchar(64) NOT NULL,
  "psp_bank_code" varchar(128) NOT NULL,
  "status" SMALLINT NOT NULL DEFAULT '1',
  "sort" int NOT NULL DEFAULT '100',
  "remark" varchar(500) DEFAULT NULL,
  "created_by" bigint DEFAULT NULL,
  "created_at" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  "updated_by" bigint DEFAULT NULL,
  "updated_at" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY ("id"),
);
CREATE UNIQUE INDEX "uk_psp_country_currency_bank" ON "psp_bank_mapping" ("psp_id","country_code","currency","bank_code");
CREATE INDEX "idx_psp_bank_code" ON "psp_bank_mapping" ("psp_id","psp_bank_code");
CREATE INDEX "idx_bank" ON "psp_bank_mapping" ("bank_code","country_code","currency");

