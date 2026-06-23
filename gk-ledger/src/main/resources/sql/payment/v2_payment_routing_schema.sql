-- 支付路由与支付方案正式结构。
-- 新项目上线使用本文件，不再使用旧的 PSP 直连路由表。

DROP TABLE IF EXISTS `payment_plan_route_option`;
DROP TABLE IF EXISTS `payment_plan_bucket`;
DROP TABLE IF EXISTS `payment_plan_catalog`;
DROP TABLE IF EXISTS `payment_route_rule`;
DROP TABLE IF EXISTS `payment_route_channel`;
DROP TABLE IF EXISTS `payment_route_group`;

CREATE TABLE `payment_route_group` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `group_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '路由组编码，例如PH_GCASH_PAYIN',
  `group_name` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '路由组名称',
  `direction` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '交易方向：PAYIN代收，PAYOUT代付',
  `country_code` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '国家/地区编码，空字符串表示通用',
  `currency` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '币种',
  `method_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '平台统一支付方式编码',
  `strategy` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PRIORITY_WEIGHT' COMMENT '组内策略：优先级加权、故障转移、最低成本、成功率优先',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：1正常，2暂停，3停用',
  `remark` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_route_group_code` (`tenant_id`, `group_code`),
  KEY `idx_payment_route_group_dimension` (`tenant_id`, `direction`, `country_code`, `currency`, `method_code`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='支付路由组';

CREATE TABLE `payment_route_channel` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `group_id` bigint NOT NULL COMMENT '支付路由组ID，对应payment_route_group.id',
  `psp_id` bigint NOT NULL COMMENT '支付服务商ID，对应psp_provider.id',
  `psp_method_id` bigint NOT NULL COMMENT '支付服务商方法ID，对应psp_method.id',
  `psp_account_id` bigint NOT NULL COMMENT '支付服务商账户ID，对应psp_account.id',
  `psp_fee_rule_id` bigint DEFAULT NULL COMMENT '支付服务商成本费率规则ID，对应psp_fee_rule.id；为空时发布阶段自动匹配',
  `priority` int NOT NULL DEFAULT 100 COMMENT '优先级，数值越小优先级越高',
  `weight` int NOT NULL DEFAULT 100 COMMENT '同优先级下的权重',
  `fallback_order` int NOT NULL DEFAULT 100 COMMENT '失败后的备用顺序',
  `min_amount` decimal(24,8) DEFAULT NULL COMMENT '最小金额',
  `max_amount` decimal(24,8) DEFAULT NULL COMMENT '最大金额',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：1正常，2暂停，3停用',
  `remark` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_route_channel_resource` (`tenant_id`, `group_id`, `psp_id`, `psp_method_id`, `psp_account_id`),
  KEY `idx_payment_route_channel_group` (`tenant_id`, `group_id`, `status`, `priority`, `fallback_order`),
  KEY `idx_payment_route_channel_psp` (`tenant_id`, `psp_id`, `psp_method_id`, `psp_account_id`),
  KEY `idx_payment_route_channel_fee_rule` (`tenant_id`, `psp_fee_rule_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='支付路由通道';

CREATE TABLE `payment_route_rule` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `rule_name` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '路由规则名称',
  `merchant_id` bigint DEFAULT NULL COMMENT '商户ID，空表示租户级默认规则',
  `merchant_app_id` bigint DEFAULT NULL COMMENT '商户应用ID，空表示不区分应用',
  `direction` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '交易方向：PAYIN代收，PAYOUT代付',
  `country_code` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '国家/地区编码，空字符串表示通用',
  `currency` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '币种',
  `method_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '平台统一支付方式编码',
  `min_amount` decimal(24,8) DEFAULT NULL COMMENT '最小金额',
  `max_amount` decimal(24,8) DEFAULT NULL COMMENT '最大金额',
  `group_id` bigint NOT NULL COMMENT '命中的支付路由组ID，对应payment_route_group.id',
  `priority` int NOT NULL DEFAULT 100 COMMENT '规则优先级，数值越小优先级越高',
  `effective_at` datetime(3) DEFAULT NULL COMMENT '生效时间',
  `expire_at` datetime(3) DEFAULT NULL COMMENT '失效时间',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：1正常，2暂停，3停用',
  `remark` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_payment_route_rule_match` (`tenant_id`, `merchant_id`, `merchant_app_id`, `direction`, `country_code`, `currency`, `method_code`, `status`, `priority`),
  KEY `idx_payment_route_rule_group` (`tenant_id`, `group_id`),
  KEY `idx_payment_route_rule_amount` (`tenant_id`, `direction`, `currency`, `method_code`, `min_amount`, `max_amount`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='支付路由规则';

CREATE TABLE `payment_plan_catalog` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `merchant_id` bigint NOT NULL COMMENT '商户ID',
  `merchant_app_id` bigint NOT NULL DEFAULT 0 COMMENT '商户应用ID，0表示不区分应用',
  `direction` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '交易方向：PAYIN代收，PAYOUT代付',
  `country_code` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '国家/地区编码，空字符串表示通用',
  `currency` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '币种',
  `method_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '平台统一支付方式编码',
  `version` bigint NOT NULL COMMENT '支付方案版本号',
  `status` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '状态：STAGING待发布，ACTIVE生效中，RETIRED已退役',
  `bucket_count` int NOT NULL DEFAULT 0 COMMENT '金额分段数量',
  `route_option_count` int NOT NULL DEFAULT 0 COMMENT '路由候选数量',
  `config_hash` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '编译后支付方案指纹',
  `compiled_at` datetime(3) NOT NULL COMMENT '编译时间',
  `activated_at` datetime(3) DEFAULT NULL COMMENT '生效时间',
  `remark` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_plan_catalog_version` (`tenant_id`, `merchant_id`, `merchant_app_id`, `direction`, `country_code`, `currency`, `method_code`, `version`),
  KEY `idx_payment_plan_catalog_active` (`tenant_id`, `merchant_id`, `merchant_app_id`, `direction`, `country_code`, `currency`, `method_code`, `status`),
  KEY `idx_payment_plan_catalog_created` (`tenant_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='支付方案目录';

CREATE TABLE `payment_plan_bucket` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `catalog_id` bigint NOT NULL COMMENT '支付方案目录ID',
  `bucket_start_amount` decimal(24,8) NOT NULL COMMENT '金额区间开始，包含',
  `bucket_end_amount` decimal(24,8) DEFAULT NULL COMMENT '金额区间结束，不包含，空表示无上限',
  `merchant_fee_rule_id` bigint NOT NULL COMMENT '商户费率规则ID',
  `merchant_fee_snapshot_json` json DEFAULT NULL COMMENT '商户费率规则快照JSON',
  `sort` int NOT NULL DEFAULT 0 COMMENT '排序值',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_payment_plan_bucket_catalog` (`tenant_id`, `catalog_id`, `sort`),
  KEY `idx_payment_plan_bucket_amount` (`tenant_id`, `catalog_id`, `bucket_start_amount`, `bucket_end_amount`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='支付方案金额分段';

CREATE TABLE `payment_plan_route_option` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `catalog_id` bigint NOT NULL COMMENT '支付方案目录ID',
  `bucket_id` bigint NOT NULL COMMENT '金额分段ID',
  `route_rule_id` bigint NOT NULL COMMENT '来源路由规则ID，对应payment_route_rule.id',
  `route_group_id` bigint NOT NULL COMMENT '来源路由组ID，对应payment_route_group.id',
  `route_channel_id` bigint NOT NULL COMMENT '来源路由通道ID，对应payment_route_channel.id',
  `psp_id` bigint NOT NULL COMMENT '支付服务商ID',
  `psp_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '支付服务商编码快照',
  `psp_method_id` bigint NOT NULL COMMENT '支付服务商方法ID',
  `psp_method_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '支付服务商方法编码快照',
  `psp_account_id` bigint NOT NULL COMMENT '支付服务商账户ID',
  `psp_account_no` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '支付服务商账户编号快照',
  `psp_fee_rule_id` bigint DEFAULT NULL COMMENT '支付服务商成本费率规则ID',
  `psp_fee_snapshot_json` json DEFAULT NULL COMMENT '支付服务商成本费率规则快照JSON',
  `route_rule_snapshot_json` json DEFAULT NULL COMMENT '支付路由规则快照JSON',
  `route_group_snapshot_json` json DEFAULT NULL COMMENT '支付路由组快照JSON',
  `route_channel_snapshot_json` json DEFAULT NULL COMMENT '支付路由通道快照JSON',
  `psp_provider_snapshot_json` json DEFAULT NULL COMMENT '支付服务商快照JSON',
  `psp_method_snapshot_json` json DEFAULT NULL COMMENT '支付服务商方法快照JSON',
  `psp_account_snapshot_json` json DEFAULT NULL COMMENT '支付服务商账户快照JSON，不包含密钥',
  `priority` int NOT NULL DEFAULT 100 COMMENT '优先级，数值越小优先级越高',
  `weight` int NOT NULL DEFAULT 100 COMMENT '同优先级下的权重',
  `fallback_order` int NOT NULL DEFAULT 100 COMMENT '失败后的备用顺序',
  `status` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE可用，DISABLED禁用',
  `sort` int NOT NULL DEFAULT 0 COMMENT '展示排序',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_payment_plan_route_bucket` (`tenant_id`, `bucket_id`, `status`, `priority`, `fallback_order`),
  KEY `idx_payment_plan_route_catalog` (`tenant_id`, `catalog_id`, `bucket_id`, `sort`),
  KEY `idx_payment_plan_route_psp` (`tenant_id`, `psp_id`, `psp_account_id`),
  KEY `idx_payment_plan_route_source` (`tenant_id`, `route_group_id`, `route_channel_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='支付方案路由候选';
