-- Payment route source configuration.
-- These tables replace the overloaded psp_route_rule model with:
-- rule = request matcher, group = channel pool, channel = PSP/method/account candidate.

ALTER TABLE `payment_plan_route_option`
  ADD COLUMN `route_group_id` bigint DEFAULT NULL COMMENT '支付路由组ID，来源 payment_route_group.id' AFTER `route_rule_id`,
  ADD COLUMN `route_channel_id` bigint DEFAULT NULL COMMENT '支付路由通道候选ID，来源 payment_route_channel.id' AFTER `route_group_id`,
  ADD KEY `idx_payment_plan_route_source` (`tenant_id`, `route_group_id`, `route_channel_id`);

CREATE TABLE `payment_route_group` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',

  `group_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '路由组编码，如 PH_GCASH_PAYIN',
  `group_name` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '路由组名称，如 菲律宾GCASH代收通道池',

  `direction` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '交易方向: PAYIN/PAYOUT',
  `country_code` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '国家/地区编码',
  `currency` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '币种',
  `method_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '平台统一支付方式编码',

  `strategy` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PRIORITY_WEIGHT' COMMENT '组内策略: PRIORITY_WEIGHT/FAILOVER/LOWEST_COST/SUCCESS_RATE',

  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态: 1正常 2暂停 3停用',
  `remark` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',

  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',

  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_route_group_code` (`tenant_id`, `group_code`),
  KEY `idx_payment_route_group_dimension` (`tenant_id`, `direction`, `country_code`, `currency`, `method_code`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='支付路由组/通道池';

CREATE TABLE `payment_route_channel` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',

  `group_id` bigint NOT NULL COMMENT '路由组ID，关联 payment_route_group.id',

  `psp_id` bigint NOT NULL COMMENT 'PSP供应商ID，关联 psp_provider.id',
  `psp_method_id` bigint NOT NULL COMMENT 'PSP方法ID，关联 psp_method.id',
  `psp_account_id` bigint NOT NULL COMMENT 'PSP账户ID，关联 psp_account.id',

  `priority` int NOT NULL DEFAULT 100 COMMENT '优先级，数字越小越优先',
  `weight` int NOT NULL DEFAULT 100 COMMENT '同优先级下权重',
  `fallback_order` int NOT NULL DEFAULT 100 COMMENT '失败后的备用顺序',

  `min_amount` decimal(24,8) DEFAULT NULL COMMENT '该通道候选最小金额',
  `max_amount` decimal(24,8) DEFAULT NULL COMMENT '该通道候选最大金额',

  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态: 1正常 2暂停 3停用',
  `remark` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',

  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',

  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_route_channel_resource` (`tenant_id`, `group_id`, `psp_id`, `psp_method_id`, `psp_account_id`),
  KEY `idx_payment_route_channel_group` (`tenant_id`, `group_id`, `status`, `priority`, `fallback_order`),
  KEY `idx_payment_route_channel_psp` (`tenant_id`, `psp_id`, `psp_method_id`, `psp_account_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='支付路由通道候选';

CREATE TABLE `payment_route_rule` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',

  `rule_name` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '路由规则名称',

  `merchant_id` bigint DEFAULT NULL COMMENT '商户ID，NULL表示租户级通用规则',
  `merchant_app_id` bigint DEFAULT NULL COMMENT '商户应用ID，NULL表示不限应用',

  `direction` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '交易方向: PAYIN/PAYOUT',
  `country_code` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '国家/地区编码',
  `currency` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '币种',
  `method_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '平台统一支付方式编码，空表示不限',

  `min_amount` decimal(24,8) DEFAULT NULL COMMENT '规则最小金额',
  `max_amount` decimal(24,8) DEFAULT NULL COMMENT '规则最大金额',

  `group_id` bigint NOT NULL COMMENT '命中的路由组ID，关联 payment_route_group.id',

  `priority` int NOT NULL DEFAULT 100 COMMENT '规则优先级，数字越小越优先',

  `effective_at` datetime(3) DEFAULT NULL COMMENT '生效时间',
  `expire_at` datetime(3) DEFAULT NULL COMMENT '失效时间',

  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态: 1正常 2暂停 3停用',
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
