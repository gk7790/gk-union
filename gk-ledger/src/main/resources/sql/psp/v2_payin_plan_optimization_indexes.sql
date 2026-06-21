-- Payin plan resolution indexes without country dimension.
-- Run this after v1_psp_schema.sql for existing environments.

ALTER TABLE `psp_route_rule`
    ADD INDEX `idx_psp_route_plan_match`
        (`tenant_id`, `currency`, `method_code`, `direction`, `status`, `merchant_id`, `merchant_app_id`, `priority`, `id`);
