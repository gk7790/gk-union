-- gk-union PostgreSQL schema generated from the provided MySQL dump.
-- Source: pasted-text.txt. Navicat metadata is intentionally omitted.

DROP TABLE IF EXISTS sys_bank CASCADE;
CREATE TABLE sys_bank (
  id bigint NOT NULL,
  country_code varchar(8) NOT NULL,
  currency varchar(8) NOT NULL,
  bank_code varchar(64) NOT NULL,
  bank_name varchar(255) NOT NULL,
  bank_short_name varchar(128) NULL,
  swift_code varchar(32) NULL,
  local_clearing_code varchar(64) NULL,
  status smallint NOT NULL DEFAULT 1,
  sort integer NOT NULL DEFAULT 100,
  remark varchar(500) NULL,
  created_by bigint NULL,
  created_at timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_by bigint NULL,
  updated_at timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  CONSTRAINT pk_sys_bank PRIMARY KEY (id)
);
COMMENT ON TABLE sys_bank IS '平台标准银行表';
COMMENT ON COLUMN sys_bank.id IS '主键ID';
COMMENT ON COLUMN sys_bank.country_code IS '国家代码，如 PH、ID、VN';
COMMENT ON COLUMN sys_bank.currency IS '币种，如 PHP、IDR、VND';
COMMENT ON COLUMN sys_bank.bank_code IS '平台标准银行编码，如 BDO、BPI、UNIONBANK';
COMMENT ON COLUMN sys_bank.bank_name IS '银行官方全称';
COMMENT ON COLUMN sys_bank.bank_short_name IS '银行简称';
COMMENT ON COLUMN sys_bank.swift_code IS 'SWIFT/BIC，可选';
COMMENT ON COLUMN sys_bank.local_clearing_code IS '本地清算码，可选';
COMMENT ON COLUMN sys_bank.status IS '状态：1启用 2暂停 3禁用';
COMMENT ON COLUMN sys_bank.sort IS '排序';
COMMENT ON COLUMN sys_bank.remark IS '备注';
CREATE UNIQUE INDEX uk_sys_bank_uk_country_currency_bank_code ON sys_bank (bank_code, country_code, currency);
CREATE INDEX idx_sys_bank_idx_country_currency_status ON sys_bank (country_code, currency, status);

DROP TABLE IF EXISTS sys_currency CASCADE;
CREATE TABLE sys_currency (
  id bigint NOT NULL,
  currency varchar(8) NOT NULL,
  currency_name varchar(128) NOT NULL,
  currency_symbol varchar(16) NULL,
  numeric_code varchar(8) NULL,
  minor_unit smallint NOT NULL DEFAULT 2,
  status smallint NOT NULL DEFAULT 1,
  sort integer NOT NULL DEFAULT 100,
  remark varchar(500) NULL,
  created_by bigint NULL,
  created_at timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_by bigint NULL,
  updated_at timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  CONSTRAINT pk_sys_currency PRIMARY KEY (id)
);
COMMENT ON TABLE sys_currency IS '系统币种字典表';
COMMENT ON COLUMN sys_currency.id IS '主键ID';
COMMENT ON COLUMN sys_currency.currency IS '币种代码，如 PHP、IDR、VND、USD';
COMMENT ON COLUMN sys_currency.currency_name IS '币种英文名称，如 Philippine Peso';
COMMENT ON COLUMN sys_currency.currency_symbol IS '币种符号，如 ₱、Rp、₫、$';
COMMENT ON COLUMN sys_currency.numeric_code IS 'ISO 4217数字代码，如 608、360、704';
COMMENT ON COLUMN sys_currency.minor_unit IS '小数位，如 PHP=2，IDR=0，VND=0';
COMMENT ON COLUMN sys_currency.status IS '状态：1启用 2暂停 3禁用';
COMMENT ON COLUMN sys_currency.sort IS '排序';
COMMENT ON COLUMN sys_currency.remark IS '备注';
COMMENT ON COLUMN sys_currency.created_by IS '创建人';
COMMENT ON COLUMN sys_currency.created_at IS '创建时间';
COMMENT ON COLUMN sys_currency.updated_by IS '更新人';
COMMENT ON COLUMN sys_currency.updated_at IS '更新时间';
CREATE UNIQUE INDEX uk_sys_currency_uk_currency ON sys_currency (currency);
CREATE INDEX idx_sys_currency_idx_status_sort ON sys_currency (status, sort);
