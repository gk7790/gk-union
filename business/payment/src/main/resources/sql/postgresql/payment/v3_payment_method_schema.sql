CREATE TABLE "payment_method" (
  "id" bigint NOT NULL,
  "method_code" varchar(64) NOT NULL,
  "method_name" varchar(128) NOT NULL,
  "method_type" varchar(32) DEFAULT NULL,
  "direction" varchar(16) DEFAULT NULL,
  "country_code" varchar(8) DEFAULT NULL,
  "currency" varchar(16) DEFAULT NULL,
  "status" SMALLINT NOT NULL DEFAULT '1',
  "sort" int NOT NULL DEFAULT '100',
  "icon_url" varchar(512) DEFAULT NULL,
  "remark" varchar(512) DEFAULT NULL,
  "created_by" bigint DEFAULT NULL,
  "created_at" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  "updated_by" bigint DEFAULT NULL,
  "updated_at" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY ("id"),
);
CREATE UNIQUE INDEX "uk_payment_method_scope" ON "payment_method" ("method_code", "country_code", "currency", "direction");
CREATE INDEX "idx_payment_method_query" ON "payment_method" ("country_code", "currency", "direction", "status", "sort");
