这 6 张表可以理解成 PSP 模块的 3 层：

```text
配置层：psp_provider / psp_method / psp_merchant / psp_route_rule
日志层：psp_request_log / psp_callback_log
业务层：pay_order / payout_order / ledger_* 后面承接
```

**psp_provider**
表示“上游三方支付公司”。

比如：

```text
GCASH_PSP
MAYA_PSP
XENDIT
PAYMONGO
```

它记录 PSP 的基础资料：

```text
psp_code
psp_name
base_url
api_version
是否支持代收
是否支持代付
状态
```

作用：告诉系统“有哪些 PSP 可以接”。

**psp_method**
表示“某个 PSP 支持的平台支付方式映射”。

比如平台统一支付方式叫：

```text
GCASH
MAYA
BANK_TRANSFER
```

但 PSP 那边可能叫：

```text
GCASH_WALLET
PH_GCASH
ewallet_gcash
```

所以要映射：

```text
平台 method_code = GCASH
PSP psp_method_code = GCASH_WALLET
country = PH
currency = PHP
direction = PAYIN
```

作用：把平台统一 API 的支付方式翻译成 PSP 自己的编码。

**psp_merchant**
表示“平台在 PSP 那边开的商户号/密钥配置”。

一个 PSP 公司下面，你可能有不同商户号：

```text
租户级 PSP 商户号
某个商户独立 PSP 商户号
不同国家/业务线 PSP 商户号
```

这里放：

```text
psp_merchant_no
api_key
api_secret
psp_public_key
callback_secret
```

作用：调用 PSP 接口时知道用哪个 PSP 商户号、哪套密钥。

**psp_route_rule**
表示“这笔订单应该走哪个 PSP”。

比如：

```text
tenant_id = 菲律宾租户
country = PH
currency = PHP
method_code = GCASH
direction = PAYIN
amount = 100
```

系统根据路由规则选出：

```text
psp_provider
psp_method
psp_merchant
```

作用：把订单从“平台统一支付方式”路由到具体 PSP。

V1 可以简单按：

```text
tenant_id
country_code
currency
method_code
direction
status = 1
amount 在 min/max 之间
priority 最小
```

取第一条。

**psp_request_log**
记录“平台请求 PSP”的完整日志。

比如：

```text
创建代收订单
创建代付订单
查询订单状态
退款请求
```

会记录：

```text
请求URL
请求头
请求体
响应状态码
响应体
耗时
是否成功
错误码
PSP订单号
```

作用：排查 PSP 接口问题、对账、追踪订单。

注意：请求体和响应体里的密钥、银行卡、手机号等敏感信息要脱敏。

**psp_callback_log**
记录“PSP 回调平台”的日志。

PSP 回调可能会：

```text
重复回调
乱序回调
签名失败
订单不存在
状态冲突
```

所以必须保存：

```text
headers
body
signature
验签状态
处理状态
PSP订单号
callback_id
body_hash
错误信息
```

作用：幂等、验签追踪、排错、审计。

**代收业务流程**

```text
1. 商户调用统一 API 创建代收订单
   ↓
2. 平台创建 pay_order
   ↓
3. 根据订单信息匹配 psp_route_rule
   ↓
4. 找到 psp_provider
   ↓
5. 找到 psp_method，把平台 method_code 转成 PSP method_code
   ↓
6. 找到 psp_merchant，拿 PSP 商户号和密钥
   ↓
7. 调用 PSP 下单接口
   ↓
8. 写 psp_request_log
   ↓
9. PSP 返回收银台链接 / 支付参数 / PSP订单号
   ↓
10. 更新 pay_order 为 PROCESSING
   ↓
11. PSP 支付成功后回调平台
   ↓
12. 写 psp_callback_log
   ↓
13. 验签、幂等、校验金额和订单
   ↓
14. 更新 pay_order 为 SUCCESS
   ↓
15. 调用 ledger 入账
   ↓
16. 写 mq_outbox
   ↓
17. 通知商户
```

**代付业务流程**

```text
1. 商户调用统一 API 创建代付订单
   ↓
2. 平台创建 payout_order
   ↓
3. 校验商户余额
   ↓
4. ledger 冻结余额，生成 ledger_hold
   ↓
5. 匹配 psp_route_rule
   ↓
6. 找到 psp_provider / psp_method / psp_merchant
   ↓
7. 调用 PSP 代付接口
   ↓
8. 写 psp_request_log
   ↓
9. 更新 payout_order 为 PROCESSING
   ↓
10. PSP 回调或定时查询结果
   ↓
11. 写 psp_callback_log 或 psp_request_log
   ↓
12. 成功：payout_order = SUCCESS，ledger 消耗冻结
   ↓
13. 失败：payout_order = FAILED，ledger 解冻
   ↓
14. 写 mq_outbox
   ↓
15. 通知商户
```

**这几张表之间的关系**

```text
psp_provider
  ↓
psp_method
  ↓
psp_route_rule
  ↑
psp_merchant
```

可以理解成：

```text
psp_provider：这是谁
psp_method：它支持什么支付方式
psp_merchant：我用什么账号和密钥调用它
psp_route_rule：什么订单走它
psp_request_log：我请求它发生了什么
psp_callback_log：它回调我发生了什么
```

PSP 模块只负责连接上游和记录交互。  
真正的钱账变化必须走 `ledger_journal / ledger_entry / ledger_balance`，不要让 PSP 模块直接改余额。