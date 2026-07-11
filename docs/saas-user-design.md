# SaaS 用户、租户与商户账号体系设计文档

## 1. 设计目标

本设计用于 SaaS 平台中的统一登录账号体系。系统中存在三类主要主体：

- 平台管理人员
- 租户
- 商户及商户层级

所有可登录账号统一存储在 `sys_user` 表中。`sys_user` 只负责登录账号、密码凭证、账号状态和基础用户信息，不直接承载租户、商户、平台身份关系。

租户、商户与用户之间的关系通过独立关联表表达。商户表 `merchant` 不存 `owner_user_id`，商户负责人、管理员、员工等身份全部通过 `merchant_user` 维护。

## 2. 核心设计原则

### 2.1 账号与业务主体分离

`sys_user` 表表示“谁可以登录系统”，不表示“这个用户属于哪个租户或商户”。

租户、商户是业务主体，应由各自的业务表维护：

- `tenant` 表表示租户主体
- `merchant` 表表示商户主体
- `tenant_user` 表表示用户与租户的关系
- `merchant_user` 表表示用户与商户的关系

这样可以避免 `sys_user` 变成混合多种业务身份的大表。

### 2.2 商户不直接存负责人用户

本设计不在 `merchant` 表中保存 `owner_user_id`、`user_id` 等用户字段。

商户负责人通过 `merchant_user.relation_type = 'OWNER'` 表达。商户管理员、员工、财务、运营等账号也通过同一张关联表表达。

这样可以支持：

- 一个商户有多个登录账号
- 一个商户有多个管理员
- 一个用户管理多个商户
- 一个商户负责人后续变更
- 商户账号禁用、解绑、换角色
- 后续扩展商户员工体系

### 2.3 权限按上下文生效

同一个用户在不同上下文下可以拥有不同权限。

例如：

- 用户 A 是平台管理员
- 用户 A 在租户 X 下是租户管理员
- 用户 A 在商户 M 下是商户运营

因此权限不应只绑定在 `sys_user` 上，而应绑定在具体业务关系上，例如 `tenant_user` 或 `merchant_user`。

## 3. 推荐表结构

### 3.1 sys_user：统一登录账号表

用于保存所有可登录账号的基础信息。

```sql
CREATE TABLE sys_user (
    id BIGINT PRIMARY KEY,
    username VARCHAR(64) NOT NULL,
    mobile VARCHAR(32),
    email VARCHAR(128),
    password_hash VARCHAR(255) NOT NULL,
    nickname VARCHAR(64),
    avatar VARCHAR(255),
    status VARCHAR(32) NOT NULL,
    last_login_time DATETIME,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    deleted_at DATETIME
);
```

字段说明：

- `username`：登录名
- `mobile`：手机号，可作为登录标识
- `email`：邮箱，可作为登录标识
- `password_hash`：密码哈希值，禁止保存明文密码
- `status`：账号状态，例如 `ACTIVE`、`DISABLED`、`LOCKED`
- `deleted_at`：软删除时间

建议唯一约束：

```sql
UNIQUE(username)
UNIQUE(mobile)
UNIQUE(email)
```

如果系统允许不同租户下账号名重复，则不要对 `sys_user.username` 做全局唯一，而应增加独立登录身份表或在登录时引入租户编码。

### 3.2 tenant：租户表

```sql
CREATE TABLE tenant (
    id BIGINT PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    code VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    deleted_at DATETIME
);
```

建议：

```sql
UNIQUE(code)
```

### 3.3 merchant：商户表

商户表只保存商户主体信息，不保存用户关系字段。

```sql
CREATE TABLE merchant (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    parent_id BIGINT,
    name VARCHAR(128) NOT NULL,
    code VARCHAR(64) NOT NULL,
    level INT,
    status VARCHAR(32) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    deleted_at DATETIME,
    INDEX idx_merchant_tenant_id (tenant_id),
    INDEX idx_merchant_parent_id (parent_id)
);
```

字段说明：

- `tenant_id`：所属租户
- `parent_id`：上级商户，用于商户层级
- `level`：商户层级，可选
- `code`：商户编码
- `status`：商户状态，例如 `ACTIVE`、`DISABLED`

建议唯一约束：

```sql
UNIQUE(tenant_id, code)
```

### 3.4 tenant_user：租户用户关系表

用于表示用户属于某个租户，以及在该租户下的身份。

```sql
CREATE TABLE tenant_user (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role_id BIGINT,
    relation_type VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    deleted_at DATETIME,
    UNIQUE(tenant_id, user_id),
    INDEX idx_tenant_user_user_id (user_id)
);
```

`relation_type` 示例：

- `OWNER`：租户负责人
- `ADMIN`：租户管理员
- `STAFF`：租户普通员工

### 3.5 merchant_user：商户用户关系表

用于表示用户属于某个商户，以及在该商户下的身份。

```sql
CREATE TABLE merchant_user (
    id BIGINT PRIMARY KEY,
    merchant_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role_id BIGINT,
    relation_type VARCHAR(32) NOT NULL,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(32) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    deleted_at DATETIME,
    UNIQUE(merchant_id, user_id),
    INDEX idx_merchant_user_user_id (user_id),
    INDEX idx_merchant_user_merchant_id (merchant_id)
);
```

`relation_type` 示例：

- `OWNER`：商户负责人
- `ADMIN`：商户管理员
- `FINANCE`：财务账号
- `OPERATOR`：运营账号
- `STAFF`：普通员工

`is_primary` 用于标记主负责人。它不是必须字段，但推荐保留，便于一个商户存在多个 `OWNER` 时指定主要联系人。

如果业务强制每个商户只能有一个主负责人，可以在业务逻辑或数据库层控制：

```text
同一个 merchant_id 下最多只能有一个 is_primary = true 且 relation_type = OWNER 的有效记录
```

### 3.6 platform_user：平台用户关系表

平台管理员也建议不要只通过 `sys_user` 字段判断，而是单独建立平台用户关系表。

```sql
CREATE TABLE platform_user (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    role_id BIGINT,
    relation_type VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    deleted_at DATETIME,
    UNIQUE(user_id)
);
```

`relation_type` 示例：

- `SUPER_ADMIN`
- `ADMIN`
- `OPERATOR`

## 4. 角色与权限设计

### 4.1 sys_role：角色表

```sql
CREATE TABLE sys_role (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT,
    role_scope VARCHAR(32) NOT NULL,
    name VARCHAR(64) NOT NULL,
    code VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    deleted_at DATETIME
);
```

`role_scope` 示例：

- `PLATFORM`
- `TENANT`
- `MERCHANT`

平台角色可以不绑定 `tenant_id`。租户角色和商户角色建议绑定 `tenant_id`，避免不同租户之间角色互相污染。

### 4.2 sys_permission：权限表

```sql
CREATE TABLE sys_permission (
    id BIGINT PRIMARY KEY,
    permission_scope VARCHAR(32) NOT NULL,
    name VARCHAR(64) NOT NULL,
    code VARCHAR(128) NOT NULL,
    type VARCHAR(32) NOT NULL,
    parent_id BIGINT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);
```

`type` 示例：

- `MENU`
- `BUTTON`
- `API`
- `DATA`

### 4.3 sys_role_permission：角色权限表

```sql
CREATE TABLE sys_role_permission (
    id BIGINT PRIMARY KEY,
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    UNIQUE(role_id, permission_id)
);
```

## 5. 登录与上下文选择流程

### 5.1 登录流程

1. 用户输入账号和密码。
2. 系统查询 `sys_user`。
3. 校验 `password_hash`。
4. 检查用户状态是否可用。
5. 查询该用户拥有的平台、租户、商户关系。
6. 如果用户只有一个可用上下文，直接进入。
7. 如果用户有多个上下文，让用户选择进入平台、某个租户或某个商户。

### 5.2 用户上下文示例

一个用户登录后可能拥有以下身份：

```text
platform_user:
- 平台管理员

tenant_user:
- 租户 A 管理员

merchant_user:
- 商户 M 负责人
- 商户 N 财务
```

系统应在登录后生成当前上下文：

```json
{
  "userId": 1001,
  "contextType": "MERCHANT",
  "tenantId": 2001,
  "merchantId": 3001,
  "roleId": 4001
}
```

后续接口鉴权基于当前上下文判断权限和数据范围。

## 6. 数据隔离设计

SaaS 系统建议大部分业务表都带上 `tenant_id`。

商户相关业务表建议同时保存：

```text
tenant_id
merchant_id
```

原因：

- `tenant_id` 用于租户级数据隔离
- `merchant_id` 用于商户级数据归属
- 查询时可以先按 `tenant_id` 限制范围，降低越权风险

示例：

```sql
CREATE TABLE order_record (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    merchant_id BIGINT NOT NULL,
    order_no VARCHAR(64) NOT NULL,
    amount DECIMAL(18,2) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at DATETIME NOT NULL,
    INDEX idx_order_tenant_id (tenant_id),
    INDEX idx_order_merchant_id (merchant_id)
);
```

## 7. 商户层级权限

如果商户存在上下级关系，需要明确上级商户是否能查看下级商户数据。

推荐设计：

- `merchant.parent_id` 只表示组织层级
- 是否能查看下级数据由权限或数据范围控制

可以在角色或关系表中增加数据范围：

```text
data_scope:
- SELF：只看当前商户
- SELF_AND_CHILDREN：当前商户及下级商户
- TENANT_ALL：租户下全部商户
```

如果放在 `merchant_user` 中：

```sql
ALTER TABLE merchant_user ADD data_scope VARCHAR(32);
```

如果放在角色中：

```sql
ALTER TABLE sys_role ADD data_scope VARCHAR(32);
```

更推荐放在角色中，因为数据范围通常属于权限配置的一部分。

## 8. 常见业务流程

### 8.1 创建商户主账号

流程：

1. 创建 `sys_user` 登录账号。
2. 创建 `merchant` 商户主体。
3. 创建 `merchant_user` 关系：

```text
merchant_id = 新商户 ID
user_id = 新用户 ID
relation_type = OWNER
is_primary = true
role_id = 商户负责人角色
status = ACTIVE
```

### 8.2 商户新增员工账号

流程：

1. 创建或选择已有 `sys_user`。
2. 插入 `merchant_user`：

```text
relation_type = STAFF / FINANCE / OPERATOR
role_id = 对应商户角色
status = ACTIVE
```

### 8.3 商户更换负责人

流程：

1. 将旧负责人 `merchant_user.is_primary` 改为 `false`，或将 `relation_type` 改为 `ADMIN`。
2. 将新负责人设置为：

```text
relation_type = OWNER
is_primary = true
```

不需要修改 `merchant` 表。

### 8.4 禁用商户账号

如果只禁用用户在某个商户下的权限：

```text
merchant_user.status = DISABLED
```

如果禁用整个登录账号：

```text
sys_user.status = DISABLED
```

两者语义不同，建议分开处理。

## 9. 不推荐设计

### 9.1 不推荐在 merchant 中保存 user_id

```text
merchant.user_id
```

问题：

- 只能自然表达一个用户
- 不适合商户多账号
- 不适合角色区分
- 不适合负责人变更留痕
- 不适合同一用户管理多个商户

### 9.2 不推荐在 sys_user 中保存 merchant_id

```text
sys_user.merchant_id
```

问题：

- 用户只能属于一个商户
- 账号体系和业务主体耦合
- 后续扩展平台、租户、商户多身份会困难

### 9.3 不推荐用 user_type 解决全部身份

```text
sys_user.user_type = PLATFORM / TENANT / MERCHANT
```

这个字段可以作为辅助显示，但不应作为核心权限来源。真实身份应从 `platform_user`、`tenant_user`、`merchant_user` 关系表查询。

## 10. 推荐最终模型

最终推荐模型如下：

```text
sys_user
  只保存登录账号和用户基础信息

tenant
  保存租户主体

merchant
  保存商户主体和商户层级
  不保存 owner_user_id
  不保存 user_id

platform_user
  保存平台人员关系

tenant_user
  保存租户人员关系

merchant_user
  保存商户人员关系
  使用 relation_type = OWNER 表示负责人
  使用 role_id 表示权限角色

sys_role / sys_permission
  保存角色和权限
```

## 11. 设计结论

推荐采用完全关系表模式：

```text
merchant 不加 owner_user_id
merchant 不加 user_id
所有商户账号关系统一放在 merchant_user
```

这是更适合 SaaS、多租户、多商户层级、多账号权限体系的设计。

该设计短期会比 `merchant.user_id` 多一张关联表，但长期更清晰、更稳定，也更容易扩展。

