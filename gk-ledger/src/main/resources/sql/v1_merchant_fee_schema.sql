-- Merchant fee rule schema for V1.
-- Merchant fee rules define how the platform charges merchants for payin and payout orders.

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS merchant_fee_rule (
    id bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    tenant_id bigint NOT NULL COMMENT '租户ID',
    merchant_id bigint NOT NULL COMMENT '商户ID',
    merchant_app_id bigint NULL DEFAULT NULL COMMENT '商户应用ID，NULL表示不限应用',
    rule_name varchar(128) NOT NULL COMMENT '规则名称',
    order_type varchar(32) NOT NULL COMMENT '订单类型: PAYIN/PAYOUT',
    country_code varchar(16) NULL DEFAULT NULL COMMENT '国家编码，NULL表示不限国家',
    currency varchar(16) NOT NULL COMMENT '币种',
    pay_channel varchar(64) NULL DEFAULT NULL COMMENT '商户支付方式，NULL表示不限支付方式',
    min_amount decimal(24,8) NULL DEFAULT NULL COMMENT '订单最小金额',
    max_amount decimal(24,8) NULL DEFAULT NULL COMMENT '订单最大金额',
    fee_mode varchar(32) NOT NULL COMMENT '手续费模式: RATE/FIXED/RATE_FIXED',
    fee_rate decimal(18,8) NOT NULL DEFAULT 0.00000000 COMMENT '比例费率，例如0.025表示2.5%',
    fee_fixed decimal(24,8) NOT NULL DEFAULT 0.00000000 COMMENT '固定手续费',
    min_fee decimal(24,8) NULL DEFAULT NULL COMMENT '最低手续费',
    max_fee decimal(24,8) NULL DEFAULT NULL COMMENT '最高手续费',
    fee_bearer varchar(32) NOT NULL DEFAULT 'MERCHANT' COMMENT '手续费承担方: MERCHANT/CUSTOMER',
    settle_mode varchar(32) NOT NULL DEFAULT 'DEDUCT' COMMENT '结算处理方式: DEDUCT/ADD',
    priority int NOT NULL DEFAULT 100 COMMENT '优先级，数字越小越优先',
    effective_at datetime(3) NULL DEFAULT NULL COMMENT '生效时间',
    expire_at datetime(3) NULL DEFAULT NULL COMMENT '失效时间',
    status tinyint NOT NULL DEFAULT 1 COMMENT '状态: 0禁用 1启用',
    remark varchar(512) NULL DEFAULT NULL COMMENT '备注',
    created_by bigint NULL DEFAULT NULL COMMENT '创建人ID',
    created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_by bigint NULL DEFAULT NULL COMMENT '更新人ID',
    updated_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_fee_rule_merchant (tenant_id, merchant_id, order_type, currency, status),
    KEY idx_fee_rule_app (tenant_id, merchant_app_id, order_type, currency, status),
    KEY idx_fee_rule_match (tenant_id, merchant_id, order_type, country_code, currency, pay_channel, status, priority),
    KEY idx_fee_rule_effective (tenant_id, status, effective_at, expire_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商户手续费规则';
