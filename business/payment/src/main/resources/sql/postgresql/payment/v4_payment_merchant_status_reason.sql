ALTER TABLE "payin_order"
  ADD COLUMN "merchant_status_code" varchar(64) NULL DEFAULT NULL,
  ADD COLUMN "merchant_status_reason" varchar(512) NULL DEFAULT NULL

ALTER TABLE "payout_order"
  ADD COLUMN "merchant_status_code" varchar(64) NULL DEFAULT NULL,
  ADD COLUMN "merchant_status_reason" varchar(512) NULL DEFAULT NULL
