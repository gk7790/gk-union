-- OpenAPI 璁よ瘉鏌ヨ浼樺寲绱㈠紩锛坢erchant_app.app_id 宸叉湁 uk_merchant_app_id锛?

ALTER TABLE merchant_api_ip_whitelist
    ADD INDEX idx_merchant_api_ip_pattern (tenant_id, merchant_id, status, ip_pattern);
