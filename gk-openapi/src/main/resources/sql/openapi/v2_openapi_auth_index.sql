-- OpenAPI 认证查询优化索引（merchant_app.app_id 已有 uk_merchant_app_id）

ALTER TABLE merchant_api_ip_whitelist
    ADD INDEX idx_merchant_api_ip_pattern (tenant_id, merchant_id, status, ip_pattern);
