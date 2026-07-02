ALTER TABLE `payin_order`
  ADD COLUMN `merchant_status_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT 'Merchant visible status reason code' AFTER `status_reason`,
  ADD COLUMN `merchant_status_reason` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT 'Merchant visible status reason message' AFTER `merchant_status_code`;

ALTER TABLE `payout_order`
  ADD COLUMN `merchant_status_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT 'Merchant visible status reason code' AFTER `status_reason`,
  ADD COLUMN `merchant_status_reason` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT 'Merchant visible status reason message' AFTER `merchant_status_code`;
