-- Payin plan resolution indexes without country dimension.
-- Run this after v1_merchant_schema.sql for existing environments.

ALTER TABLE `merchant_fee_rule`
    ADD INDEX `idx_fee_rule_order_match`
        (`tenant_id`, `merchant_id`, `merchant_app_id`, `direction`, `currency`, `method_code`, `status`, `priority`, `id`);
