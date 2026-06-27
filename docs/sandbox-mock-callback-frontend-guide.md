# 商户后台沙箱 Mock 回调前端接入说明

本文档说明商户后台代收、代付订单中的沙箱 Mock 回调按钮何时展示、如何调用接口、接口返回什么，以及前端应该如何展示发送给商户的参数和商户响应数据。

## 1. 功能定位

沙箱 Mock 回调用于测试应用订单模拟 PSP 终态回调。

前端点击后，后端会：

1. 将沙箱订单推进到终态：成功或失败。
2. 创建商户通知任务。
3. 同步 POST 商户订单的 `notify_url`。
4. 返回订单 Mock 结果、发送给商户的请求参数、通知地址、商户响应内容，供前端展示。

注意：商户通知失败时，接口顶层仍然可能返回 `code=0`。这是因为 `code=0` 表示 Mock 接口自身执行完成，不代表商户通知成功。商户通知是否成功要看 `data.notify.acknowledged`。

## 2. 前端按钮展示条件

### 2.1 显示 Mock 回调按钮

建议同时满足以下条件时展示：

| 条件 | 说明 |
| --- | --- |
| `pspCode == "SANDBOX"` | 只允许沙箱订单 |
| `appEnv == "TEST"` | 只允许测试应用订单 |
| `status == "PROCESSING"` | 只有处理中订单允许推进终态 |
| 有 `payment:merchant-notify-task:resend` 权限 | 后端接口权限 |

按钮文案建议：

```text
Mock 回调
```

点击后让用户选择：

- Mock 成功
- Mock 失败，并填写失败原因

### 2.2 终态订单显示重发通知

如果订单已经是终态，不建议继续显示为 Mock 回调，因为后端不会再次修改订单状态，只会尝试重发商户通知。

代收终态：

```text
SUCCESS / FAILED / CLOSED
```

代付终态：

```text
SUCCESS / FAILED / CANCELLED
```

按钮文案建议：

```text
重发商户通知
```

### 2.3 不建议展示的状态

以下状态不建议展示 Mock 回调：

```text
CREATED / MANUAL_REVIEW / 其他非 PROCESSING 且非终态状态
```

这些状态调用后端会被拒绝，提示类似：

```text
仅处理中订单可 mock 回调
```

## 3. 接口地址

### 3.1 代收订单

```http
POST /pay-order/{id}/sandbox/mock-callback
```

### 3.2 代付订单

```http
POST /payout-order/{id}/sandbox/mock-callback
```

路径参数：

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `id` | number | 是 | 订单数据库 ID |

权限：

```text
payment:merchant-notify-task:resend
```

## 4. 请求参数

Body：

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `outcome` | string | 是 | Mock 结果，只能是 `SUCCESS` 或 `FAILED` |
| `failReason` | string | 否 | 失败原因。`outcome=FAILED` 时建议填写，后端最长保留 500 字符 |

成功示例：

```json
{
  "outcome": "SUCCESS"
}
```

失败示例：

```json
{
  "outcome": "FAILED",
  "failReason": "Sandbox payment failed"
}
```

## 5. 响应结构

顶层为统一响应：

```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "orderType": "PAY",
    "systemOrderNo": "P202606270001",
    "merchantOrderNo": "M202606270001",
    "status": "SUCCESS",
    "changed": true,
    "message": "沙箱代收回调成功",
    "outcome": "SUCCESS",
    "failReason": null,
    "notify": {
      "sent": true,
      "acknowledged": true,
      "notifyUrl": "https://merchant.example.com/notify",
      "requestBody": "{\"merchant_id\":\"M10001\"}",
      "requestSignature": "abc123",
      "httpStatus": 200,
      "responseBody": "success",
      "errorMessage": null,
      "costMs": 123,
      "notifyTaskId": 10001,
      "taskNo": "NT202606270001",
      "attemptNo": 1
    }
  }
}
```

## 6. `data` 字段说明

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `orderType` | string | 订单类型：`PAY` 代收，`PAYOUT` 代付 |
| `systemOrderNo` | string | 平台订单号 |
| `merchantOrderNo` | string | 商户订单号 |
| `status` | string | Mock 后订单状态 |
| `changed` | boolean | 本次是否修改了订单状态。终态订单重发通知时通常为 `false` |
| `message` | string | 后端处理说明 |
| `outcome` | string | 本次 Mock 入参结果：`SUCCESS` 或 `FAILED` |
| `failReason` | string | Mock 失败原因。成功时通常为 `null` |
| `notify` | object | 商户通知发送结果 |

## 7. `data.notify` 字段说明

这部分是前端展示商户通知链路的核心。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `sent` | boolean | 是否实际发起了 HTTP 通知 |
| `acknowledged` | boolean | 平台是否认为商户已确认成功 |
| `notifyUrl` | string | 商户通知地址，也就是本次 POST 的 URL |
| `requestBody` | string | 实际发送给商户的请求体 JSON 字符串 |
| `requestSignature` | string | 本次通知签名 |
| `httpStatus` | number | 商户接口返回的 HTTP 状态码 |
| `responseBody` | string | 商户接口返回内容 |
| `errorMessage` | string | 通知失败原因 |
| `costMs` | number | 本次通知耗时，单位毫秒 |
| `notifyTaskId` | number | 商户通知任务 ID |
| `taskNo` | string | 商户通知任务号 |
| `attemptNo` | number | 本次是第几次通知尝试 |

`attemptNo` 说明：

```text
attemptNo = 当前通知任务 retryCount + 1
```

例如 `attemptNo=3` 表示这是第 3 次向商户发送通知。

## 8. 商户通知成功和失败判断

### 8.1 Mock 接口是否执行成功

看顶层：

```text
code == 0
```

### 8.2 商户是否确认通知成功

看通知结果：

```text
data.notify.acknowledged == true
```

当前后端成功判断规则：

1. 商户接口 HTTP 状态码是 2xx。
2. 商户响应 body 非空。
3. 响应 body 忽略大小写后包含 `success` 或 `ok`。

注意：如果商户返回 HTTP 500、超时、连接失败，或者响应 body 不包含 `success` / `ok`，都会认为 `acknowledged=false`。

## 9. 商户通知失败示例

商户返回 HTTP 500：

```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "orderType": "PAY",
    "systemOrderNo": "P202606270001",
    "merchantOrderNo": "M202606270001",
    "status": "SUCCESS",
    "changed": true,
    "message": "沙箱代收回调成功",
    "outcome": "SUCCESS",
    "failReason": null,
    "notify": {
      "sent": true,
      "acknowledged": false,
      "notifyUrl": "https://merchant.example.com/notify",
      "requestBody": "{\"merchant_id\":\"M10001\"}",
      "requestSignature": "abc123",
      "httpStatus": 500,
      "responseBody": "server error",
      "errorMessage": "http status 500",
      "costMs": 98,
      "notifyTaskId": 10001,
      "taskNo": "NT202606270001",
      "attemptNo": 1
    }
  }
}
```

前端应展示为：

```text
Mock 回调成功，商户通知失败
HTTP 状态码：500
商户响应：server error
失败原因：http status 500
```

## 10. 未发送通知示例

如果没有通知任务，或通知任务正在处理，可能返回：

```json
{
  "sent": false,
  "acknowledged": false,
  "errorMessage": "该订单暂无商户通知任务"
}
```

或：

```json
{
  "sent": false,
  "acknowledged": false,
  "errorMessage": "通知任务正在处理，请稍后再试"
}
```

前端应展示 `errorMessage`，不要当作系统异常处理。

## 11. 平台发送给商户的请求体

`data.notify.requestBody` 是最终实际发送给商户的 JSON 字符串，签名字段也会写入 body。

### 11.1 代收 PAY 通知示例

```json
{
  "merchant_id": "M10001",
  "app_id": "APP_TEST_001",
  "order_type": "PAY",
  "system_order_id": "P202606270001",
  "merchant_order_id": "M202606270001",
  "currency": "PHP",
  "amount": "100.00",
  "order_status": "PAY_SUCCESS",
  "msg": "Transaction success",
  "paid_amount": "100.00",
  "settle_amount": "98.00",
  "fee_amount": "2.00",
  "sign": "abc123"
}
```

### 11.2 代付 PAYOUT 通知示例

```json
{
  "merchant_id": "M10001",
  "app_id": "APP_TEST_001",
  "order_type": "PAYOUT",
  "system_order_id": "PO202606270001",
  "merchant_order_id": "M202606270001",
  "currency": "PHP",
  "amount": "100.00",
  "order_status": "PAYOUT_FAILED",
  "msg": "Sandbox payment failed",
  "debit_amount": "102.00",
  "fee_amount": "2.00",
  "sign": "abc123"
}
```

实际展示时以前端拿到的 `data.notify.requestBody` 为准。

## 12. 前端展示建议

建议弹窗或抽屉分成两块。

### 12.1 Mock 回调结果

展示字段：

| 展示名 | 字段 |
| --- | --- |
| 订单类型 | `data.orderType` |
| 平台订单号 | `data.systemOrderNo` |
| 商户订单号 | `data.merchantOrderNo` |
| Mock 结果 | `data.outcome` |
| 当前订单状态 | `data.status` |
| 是否变更状态 | `data.changed` |
| 处理说明 | `data.message` |
| 失败原因 | `data.failReason` |

### 12.2 商户通知结果

展示字段：

| 展示名 | 字段 |
| --- | --- |
| 是否发送 | `data.notify.sent` |
| 商户是否确认 | `data.notify.acknowledged` |
| 通知地址 | `data.notify.notifyUrl` |
| 请求参数 | `data.notify.requestBody` |
| 请求签名 | `data.notify.requestSignature` |
| HTTP 状态码 | `data.notify.httpStatus` |
| 商户响应 | `data.notify.responseBody` |
| 失败原因 | `data.notify.errorMessage` |
| 通知耗时 | `data.notify.costMs` |
| 通知任务号 | `data.notify.taskNo` |
| 本次通知次数 | `data.notify.attemptNo` |

### 12.3 状态文案建议

| 条件 | 前端文案 |
| --- | --- |
| `code == 0 && notify.acknowledged == true` | Mock 回调成功，商户通知成功 |
| `code == 0 && notify.sent == true && notify.acknowledged == false` | Mock 回调成功，商户通知失败 |
| `code == 0 && notify.sent == false` | Mock 回调成功，商户通知未发送 |
| `code != 0` | Mock 回调失败 |

## 13. 前端处理伪代码

```ts
const res = await sandboxMockCallback(orderId, {
  outcome: "SUCCESS"
});

if (res.code !== 0) {
  showError(res.msg || "Mock 回调失败");
  return;
}

const result = res.data;
const notify = result?.notify;

showMockResult({
  orderType: result.orderType,
  systemOrderNo: result.systemOrderNo,
  merchantOrderNo: result.merchantOrderNo,
  status: result.status,
  changed: result.changed,
  outcome: result.outcome,
  failReason: result.failReason,
  message: result.message
});

showNotifyResult({
  sent: notify?.sent,
  acknowledged: notify?.acknowledged,
  notifyUrl: notify?.notifyUrl,
  requestBody: notify?.requestBody,
  requestSignature: notify?.requestSignature,
  httpStatus: notify?.httpStatus,
  responseBody: notify?.responseBody,
  errorMessage: notify?.errorMessage,
  costMs: notify?.costMs,
  taskNo: notify?.taskNo,
  attemptNo: notify?.attemptNo
});
```

