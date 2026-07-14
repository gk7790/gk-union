-- gk-union PostgreSQL schema generated from the provided MySQL dump.
-- Source: pasted-text.txt. MySQL comments and Navicat metadata are intentionally omitted.

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
CREATE UNIQUE INDEX uk_sys_currency_uk_currency ON sys_currency (currency);
CREATE INDEX idx_sys_currency_idx_status_sort ON sys_currency (status, sort);
