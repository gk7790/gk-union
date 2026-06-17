# 商户 OpenAPI V1 接口文档

## 1. 概述

本文档用于商户对接平台代收、代付、查单、余额查询和支付方式查询接口。

V1 接口统一使用：

- 请求方式：`POST`
- 请求格式：`application/json`
- 字符编码：`UTF-8`
- 签名算法：默认 `HMAC-SHA256`，兼容 `MD5`
- 接口前缀：`/api/v1`

所有接口都需要通过商户应用鉴权。商户不能传 `tenant_id`，平台会根据 `app_id` 自动识别租户、商户和应用。

### 1.1 字段命名规范

Open API 请求、响应及异步通知的 JSON 字段**统一使用 snake_case（下划线）**，例如：

- `app_id`、`merchant_order_id`、`system_order_id`
- `country_code`、`method_code`、`notify_url`、`return_url`
- `pay_url`、`account_no`

不支持 camelCase 别名（如 `merchantOrderId`）。签名时以 JSON 中的实际字段名参与排序拼接。

## 2. 公共参数

所有接口请求参数中都需要包含以下公共参数：

| 参数名 | 必填 | 类型 | 说明 |
|---|---:|---|
| `app_id` | 是 | string | 商户应用 ID，由平台后台创建 |
| `timestamp` | 是 | string | 毫秒时间戳，服务端默认允许 5 分钟误差 |
| `nonce` | 否 | string | 可选随机字符串；传了会参与签名，同一 App 在有效期内不可重复 |
| `sign` | 是 | string | 请求签名 |
| `sign_type` | 否 | string | 签名方式，默认 `HMAC_SHA256`；兼容值：`MD5` |

请求头只需要声明内容类型：

```http
Content-Type: application/json
```

## 3. 签名规则

### 3.1 签名步骤

平台当前 V1 推荐使用“参数名 ASCII 升序排序 + HMAC-SHA256”，同时兼容传统支付文档常见的 `MD5` 签名。

签名生成步骤：

1. 获取所有请求参数，包含公共参数和业务参数。
2. 去掉 `sign`、`signature` 参数。
3. 去掉值为 `null`、空字符串的参数。
4. 按参数名 ASCII 码从小到大排序。
5. 按 `key=value&key=value` 格式拼接成 `stringA`。
6. 默认使用商户应用密钥 `api_secret` 计算 `HMAC_SHA256(stringA, api_secret)`。
7. 将签名结果转成小写 hex，得到 `sign`。

### 3.2 签名示例

请求参数：

```json
{
  "app_id": "APP_PH_MANILA_001",
  "timestamp": "1780800000000",
  "merchant_order_id": "M202606080001",
  "amount": "100.00",
  "method_code": "GCASH",
  "notify_url": "https://merchant.example.com/notify",
  "return_url": "https://merchant.example.com/return"
}
```

排序后拼接得到 `stringA`：

```text
amount=100.00&app_id=APP_PH_MANILA_001&merchant_order_id=M202606080001&notify_url=https://merchant.example.com/notify&return_url=https://merchant.example.com/return&method_code=GCASH&timestamp=1780800000000
```

计算签名：

```text
sign = HMAC_SHA256(stringA, api_secret).toLowerCase()
```

如果商户应用配置为 `MD5`，请求需传 `sign_type=MD5`，签名方式为：

```text
stringB = stringA + "&key=" + api_secret
sign = MD5(stringB).toLowerCase()
```

注意：

- `sign` 不参与签名。
- `null` 和空字符串不参与签名。
- `nonce` 可选；如果传入，必须参与签名，同一个 `app_id + nonce` 在有效期内重复使用会被判定为重放请求。
- 嵌套对象参数不建议参与签名；V1 推荐商户请求参数保持扁平。

## 4. 公共响应结构

成功响应：

```json
{
  "code": "SUCCESS",
  "message": "success",
  "timestamp": 1780800000000,
  "data": {}
}
```

失败响应：

```json
{
  "code": "INVALID_SIGNATURE",
  "message": "Invalid signature",
  "timestamp": 1780800000000,
  "data": null
}
```

## 5. 创建代收订单

### 请求信息

```http
POST /api/v1/pay/create
```

### 请求参数

| 参数名 | 必填 | 类型 | 说明 |
|---|---:|---|---|
| `merchant_order_id` | 是 | string | 商户订单号，商户侧唯一 |
| `amount` | 是 | decimal/string | 订单金额 |
| `currency` | 否 | string | 币种，例如 `PHP`；不传时可由商户或支付方式默认配置决定 |
| `country_code` | 否 | string | 国家编码，例如 `PH`；不传时可由商户或支付方式默认配置决定 |
| `method_code` | 是 | string | 支付方式，例如 `GCASH`、`MAYA` |
| `subject` | 否 | string | 订单标题 |
| `description` | 否 | string | 订单描述 |
| `notify_url` | 是 | string | 商户异步通知地址 |
| `return_url` | 是 | string | 支付完成后的前端跳转地址 |
| `payer` | 否 | object | 付款人扩展信息 |
| `extra` | 否 | object | 商户扩展参数 |

### 请求示例

```json
{
  "merchant_order_id": "M202606080001",
  "amount": "100.00",
  "method_code": "GCASH",
  "notify_url": "https://merchant.example.com/notify",
  "return_url": "https://merchant.example.com/return",
  "app_id": "APP_PH_MANILA_001",
  "timestamp": "1780800000000",
  "sign": "7c1a..."
}
```

### 响应参数

| 参数名 | 类型 | 说明 |
|---|---|---|
| `system_order_id` | string | 平台代收订单号 |
| `merchant_order_id` | string | 商户订单号 |
| `status` | string | 订单状态 |
| `status_reason` | string | 状态原因 |
| `amount` | decimal | 订单金额 |
| `currency` | string | 币种 |
| `country_code` | string | 国家编码 |
| `method_code` | string | 支付方式 |
| `pay_url` | string | 收银台/支付链接 |

### 响应示例

`balance` 表示商户可用余额。冻结余额、待结算余额等账务内部余额仅在后台管理端查看。

```json
{
  "code": "SUCCESS",
  "message": "success",
  "timestamp": 1780800000000,
  "data": {
    "system_order_id": "PAY202606080001",
    "merchant_order_id": "M202606080001",
    "status": "PROCESSING",
    "amount": "100.00",
    "currency": "PHP",
    "country_code": "PH",
    "method_code": "GCASH",
    "pay_url": "https://psp.example.com/pay/xxx"
  }
}
```

## 6. 查询代收订单

### 请求信息

```http
POST /api/v1/pay/query
```

### 请求参数

二选一传入：

| 参数名 | 必填 | 类型 | 说明 |
|---|---:|---|---|
| `system_order_id` | 否 | string | 平台代收订单号 |
| `merchant_order_id` | 否 | string | 商户订单号 |

### 请求示例

```json
{
  "app_id": "APP_PH_MANILA_001",
  "timestamp": "1780800000000",
  "merchant_order_id": "M202606080001"
  "sign": "7c1a..."
}
```

响应 `data` 结构同创建代收订单。

## 7. 创建代付订单

### 请求信息

```http
POST /api/v1/payout/create
```

### 请求参数

| 参数名 | 必填 | 类型 | 说明 |
|---|---:|---|---|
| `merchant_order_id` | 是 | string | 商户订单号，商户侧唯一 |
| `amount` | 是 | decimal/string | 代付金额 |
| `currency` | 是 | string | 币种，例如 `PHP` |
| `country_code` | 是 | string | 国家编码，例如 `PH` |
| `method_code` | 是 | string | 代付方式，例如 `GCASH`、`MAYA` |
| `purpose` | 否 | string | 代付用途 |
| `notify_url` | 否 | string | 商户异步通知地址 |
| `payee` | 是 | object | 收款人信息 |
| `extra` | 否 | object | 商户扩展参数 |

`payee` 参数：

| 参数名 | 必填 | 类型 | 说明 |
|---|---:|---|---|
| `name` | 是 | string | 收款人姓名 |
| `account_no` | 是 | string | 收款账号/钱包号 |
| `bank_code` | 否 | string | 银行编码 |
| `wallet_type` | 否 | string | 钱包类型 |
| `phone` | 否 | string | 手机号 |
| `email` | 否 | string | 邮箱 |

`payee` 支持透传国家或 PSP 特有字段，例如 `document_type`、`document_no`、`branch_code`、`bank_cci`、`account_card_type` 等。系统会优先使用标准字段，其他字段会进入代付订单的 `payee_json`，供具体 PSP 适配器读取。


### 请求示例

```json
{
  "merchant_order_id": "PO202606080001",
  "amount": "100.00",
  "currency": "PHP",
  "country_code": "PH",
  "method_code": "GCASH",
  "purpose": "withdraw",
  "notify_url": "https://merchant.example.com/notify",
  "app_id": "APP_PH_MANILA_001",
  "timestamp": "1780800000000",
  "sign": "7c1a...",
  "payee": {
    "name": "Juan",
    "account_no": "09170000000",
    "wallet_type": "GCASH",
    "phone": "09170000000"
  }
}
```

### 响应参数

| 参数名 | 类型 | 说明 |
|---|---|---|
| `system_order_id` | string | 平台代付订单号 |
| `merchant_order_id` | string | 商户订单号 |
| `status` | string | 订单状态 |
| `status_reason` | string | 状态原因 |
| `amount` | decimal | 代付金额 |
| `currency` | string | 币种 |
| `country_code` | string | 国家编码 |
| `method_code` | string | 代付方式 |

## 8. 查询代付订单

### 请求信息

```http
POST /api/v1/payout/query
```

### 请求参数

二选一传入：

| 参数名 | 必填 | 类型 | 说明 |
|---|---:|---|---|
| `system_order_id` | 否 | string | 平台代付订单号 |
| `merchant_order_id` | 否 | string | 商户订单号 |

### 请求示例

```json
{
  "app_id": "APP_PH_MANILA_001",
  "timestamp": "1780800000000",
  "merchant_order_id": "PO202606080001"
  "sign": "7c1a..."
}
```

响应 `data` 结构同创建代付订单。

## 9. 查询余额

### 请求信息

```http
POST /api/v1/balance
```

### 请求参数

| 参数名 | 必填 | 类型 | 说明 |
|---|---:|---|---|
| `currency` | 否 | string | 币种，不传则查询全部币种 |

### 请求示例

```json
{
  "app_id": "APP_PH_MANILA_001",
  "timestamp": "1780800000000",
  "currency": "PHP",
  "sign": "7c1a..."
}
```

### 响应示例

```json
{
  "code": "SUCCESS",
  "message": "success",
  "timestamp": 1780800000000,
  "data": [
    {
      "account_no": "ACC_MERCHANT_PHP_001",
      "currency": "PHP",
      "balance": "1000.00"
    }
  ]
}
```

## 10. 查询支付方式

### 请求信息

```http
POST /api/v1/methods
```

### 请求参数

| 参数名 | 必填 | 类型 | 说明 |
|---|---:|---|---|
| `country_code` | 否 | string | 国家编码，例如 `PH` |
| `currency` | 否 | string | 币种，例如 `PHP` |
| `direction` | 否 | string | 方向：`PAYIN` / `PAYOUT` |

### 请求示例

```json
{
  "app_id": "APP_PH_MANILA_001",
  "timestamp": "1780800000000",
  "country_code": "PH",
  "currency": "PHP",
  "direction": "PAYIN",
  "sign": "7c1a..."
}
```

### 响应示例

```json
{
  "code": "SUCCESS",
  "message": "success",
  "timestamp": 1780800000000,
  "data": [
    {
      "method_code": "GCASH",
      "method_name": "GCash",
      "country_code": "PH",
      "currency": "PHP",
      "direction": "PAYIN",
      "min_amount": "10.00",
      "max_amount": "50000.00"
    }
  ]
}
```

## 11. 错误码

| 错误码 | 说明 |
|---|---|
| `SUCCESS` | 成功 |
| `INVALID_REQUEST` | 请求参数错误 |
| `INVALID_APP` | 应用不存在或配置异常 |
| `APP_DISABLED` | 应用已禁用 |
| `MERCHANT_DISABLED` | 商户不可用 |
| `INVALID_IP` | 请求 IP 不在白名单 |
| `INVALID_TIMESTAMP` | 时间戳无效或超出允许窗口 |
| `REPLAY_REQUEST` | 重放请求，nonce 已使用；仅在请求传入 nonce 时可能出现 |
| `INVALID_SIGNATURE` | 签名错误 |
| `UNSUPPORTED_SIGN_TYPE` | 不支持的签名类型 |
| `DUPLICATE_REQUEST` | 重复请求 |
| `INVALID_AMOUNT` | 金额无效 |
| `UNSUPPORTED_METHOD` | 不支持的支付方式 |
| `INSUFFICIENT_BALANCE` | 余额不足 |
| `ORDER_NOT_FOUND` | 订单不存在 |
| `ORDER_STATUS_INVALID` | 订单状态不允许当前操作 |
| `SERVICE_NOT_READY` | 服务流程尚未接入完成 |
| `SYSTEM_ERROR` | 系统异常 |

## 12. 订单状态

代收订单常见状态：

| 状态 | 说明 |
|---|---|
| `CREATED` | 已创建 |
| `PROCESSING` | 处理中 |
| `SUCCESS` | 支付成功 |
| `FAILED` | 支付失败 |
| `CLOSED` | 已关闭 |

代付订单常见状态：

| 状态 | 说明 |
|---|---|
| `CREATED` | 已创建 |
| `FROZEN` | 资金已冻结 |
| `PROCESSING` | 处理中 |
| `SUCCESS` | 代付成功 |
| `FAILED` | 代付失败 |
| `CANCELLED` | 已取消 |

## 13. 异步通知

订单终态（成功/失败）后，平台向创建订单时传入的 `notify_url` 发送 POST JSON 通知。字段同样使用 snake_case，与 Open API 保持一致。

| 参数名 | 说明 |
|---|---|
| `merchant_id` | 商户号 |
| `app_id` | 应用 ID |
| `order_type` | `PAY` / `PAYOUT` |
| `system_order_id` | 平台订单号 |
| `merchant_order_id` | 商户订单号 |
| `currency` | 币种 |
| `amount` | 订单金额 |
| `order_status` | 订单状态 |
| `msg` | 状态说明 |
| `paid_amount` | 代收实付金额（代收） |
| `settle_amount` | 结算金额（代收，可选） |
| `debit_amount` | 扣款金额（代付） |
| `fee_amount` | 手续费（可选） |
| `sign` | 签名，发送时注入 |

商户收到通知后应返回 HTTP 200 且 body 为 `success`（大小写不敏感）。

## 14. 对接建议

- V1 默认不要求传 `nonce`；如果商户传入 `nonce`，每次请求应生成新的值。
- 创建订单时，`merchant_order_id` 在同一商户下必须唯一，并承担幂等职责。
- 商户应保存平台订单号和商户订单号，用于问题排查。
- 生产环境应配置固定服务器出口 IP，并在平台后台维护 IP 白名单。
- `app_id`、`timestamp`、业务参数都参与签名；如果传入 `nonce`，`nonce` 也参与签名；`sign` 不参与签名。
