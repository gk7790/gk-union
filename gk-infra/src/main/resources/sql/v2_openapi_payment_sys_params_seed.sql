INSERT INTO `sys_params`
(`id`, `param_code`, `param_value`, `param_type`, `remark`, `created_at`)
SELECT 3000000000000000002, 'GK_OPENAPI_CONFIG_KEY', '{"payoutAsyncSubmit":true,"requireNonce":true,"defaultSignType":"HMAC_SHA256"}', 1, 'OpenAPI runtime config', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_params` WHERE `param_code` = 'GK_OPENAPI_CONFIG_KEY');

INSERT INTO `sys_params`
(`id`, `param_code`, `param_value`, `param_type`, `remark`, `created_at`)
SELECT 3000000000000000003, 'PSP_CALLBACK_CONFIG_KEY', '{"baseUrl":"https://callback.example.com"}', 1, 'PSP callback config', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_params` WHERE `param_code` = 'PSP_CALLBACK_CONFIG_KEY');

INSERT INTO `sys_params`
(`id`, `param_code`, `param_value`, `param_type`, `remark`, `created_at`)
SELECT 3000000000000000004, 'MERCHANT_DEFAULT_CONFIG_KEY', '{"timezone":"UTC","lang":"zh-CN","countryCode":"PH","configJson":{}}', 1, 'Merchant default config', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_params` WHERE `param_code` = 'MERCHANT_DEFAULT_CONFIG_KEY');

INSERT INTO `sys_params`
(`id`, `param_code`, `param_value`, `param_type`, `remark`, `created_at`)
SELECT 3000000000000000005, 'MERCHANT_NOTIFY_CONFIG_KEY', '{"connectTimeoutMs":3000,"readTimeoutMs":5000,"successTokens":["success","ok"],"maxRetryCount":16}', 1, 'Merchant notify config', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_params` WHERE `param_code` = 'MERCHANT_NOTIFY_CONFIG_KEY');

INSERT INTO `sys_params`
(`id`, `param_code`, `param_value`, `param_type`, `remark`, `created_at`)
SELECT 3000000000000000006, 'PSP_QUERY_CONFIG_KEY', '{"maxQueryCount":30,"backoffSeconds":[60,120,300,600,900,1800]}', 1, 'PSP query compensation config', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_params` WHERE `param_code` = 'PSP_QUERY_CONFIG_KEY');

INSERT INTO `sys_params`
(`id`, `param_code`, `param_value`, `param_type`, `remark`, `created_at`)
SELECT 3000000000000000007, 'PAYOUT_SUBMIT_CONFIG_KEY', '{"asyncSubmit":true,"defaultBatchSize":20,"firstQueryDelaySeconds":60}', 1, 'Payout submit config', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_params` WHERE `param_code` = 'PAYOUT_SUBMIT_CONFIG_KEY');
