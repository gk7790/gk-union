# Philippines Four-Party Payment Aggregator V1 Design

## 1. 背景与目标

本设计面向菲律宾市场建设四方支付聚合平台。平台对下游商户开放统一 API，对上游接入多个 PSP 三方支付公司；PSP 已经接入本地支付方式，例如 GCash、Maya、银行转账、QRPH 或其他菲律宾本地支付方式。

V1 定位为商户支付与清结算平台，支持代收、代付、商户账务余额、费率、对账、差错处理、结算报表和运营后台管理。V1 不提供终端用户钱包，不直接对接 GCash 或 Maya 官方接口，不做退款、平台垫资、换汇和智能路由。

## 2. 当前项目扫描结论

当前 `gk-union` 是 Spring Boot 3.5、Java 21、MyBatis-Plus 的多模块后台项目。现有模块包括 `gk-common`、`gk-infra`、`gk-auth`、`gk-platform`、`gk-tenant`、`gk-scheduler`、`gk-admin` 等。

现有后台接口风格为：

- Controller 返回 `R.ok()` / `R.error(...)`。
- 分页返回 `PageData`。
- 查询参数通过 `@RequestMap DynMap` 接收。
- Service/Dao 使用 `BaseServiceImpl`、`CrudServiceImpl`、`BaseDao`。
- 后台权限使用 `@RequiresPermission`。
- 登录态和租户上下文通过 `ReqContextHolder` 传递。
- `FieldMetaObjectHandler` 已支持 `tenantId`、`createdBy`、`createdAt`、`updatedBy`、`updatedAt` 自动填充。
- `RedisUtils` 可用于 nonce 防重放、幂等辅助和通知重试。
- `gk-scheduler` 已有 Quartz 能力，可用于释放计划、结算批次、对账处理和通知重试。

`gk-ledger` 目录已经存在，但当前基本为空壳，并且根 `pom.xml` 尚未加入 `<module>gk-ledger</module>`，`gk-admin` 也尚未依赖 `gk-ledger`。V1 应优先补强 `gk-ledger`，并通过 `gk-admin` 聚合启动。

## 3. V1 范围

V1 包含：

- SaaS 多租户和多业务线基础上的支付商户管理。
- 商户应用、API 密钥、IP 白名单、回调地址管理。
- 统一商户开放 API。
- 代收订单、代付订单。
- PSP 管理、PSP 支付方式、PSP 路由规则。
- 商户费率和结算配置。
- 强账务核心：资金主体、账户、凭证、分录。
- 多国家、多币种余额隔离。
- 代收冻结、释放到可用余额。
- 代付余额校验、冻结、成功确认支出、失败解冻。
- 文件导入对账。
- 差错处理和人工调账。
- 结算批次、结算明细、结算报表导出。
- 商户通知、PSP 请求日志、PSP 回调日志、操作审计。

V1 不包含：

- 终端用户钱包。
- 用户储值账户。
- 直接接入 GCash 或 Maya 官方接口。
- 退款 API。
- 平台垫资或授信代付。
- 跨币种换汇。
- 多级审批流。
- 自动智能路由。
- 自动拉取 PSP 对账单。

## 4. 术语与命名

系统内统一使用 `psp` 表示上游三方支付公司。避免使用 `channel` 表示三方，因为四方支付场景中 `channel` 容易和支付通道、支付方式混淆。

核心命名如下：

- `sys_tenant`：SaaS 租户、业务线或组织空间。
- `merchant`：支付商户主体。
- `merchant_app`：商户 API 接入应用。
- `pay_method`：平台统一支付方式，例如 `GCASH`、`MAYA`、`BANK_TRANSFER`、`QRPH`。
- `psp_provider`：上游三方支付公司。
- `psp_method`：某个 PSP 支持的支付方式映射。
- `pay_order`：代收订单。
- `payout_order`：代付订单。
- `ledger_*`：账务核心。
- `settle_*`：清分结算。
- `recon_*`：对账和差错。
- `notify_*`：商户通知。
- `audit_*`：操作审计。

## 5. SaaS、租户与商户模型

系统支持 SaaS 多租户和多业务线，因此 `sys_tenant` 不直接等于商户。租户和商户关系如下：

```text
sys_tenant 1 - N merchant
merchant 1 - N merchant_app
```

`sys_tenant` 表示平台中的租户、业务线或组织空间。`merchant` 表示支付商户主体。`merchant_app` 表示商户的 API 接入身份，每个应用有独立 `app_id`、`api_secret`、IP 白名单和回调地址。

交易类表统一保留：

```text
tenant_id
merchant_id
app_id
country_code
currency
```

这样可以支持同一租户下多个商户、同一商户多个应用、同一商户跑多个国家和多个币种。

## 6. 工程结构

V1 不新增多个 Maven 模块，优先在现有 `gk-ledger` 中按包拆分业务边界：

```text
gk-ledger
  com.gk.ledger.merchant
  com.gk.ledger.payment
  com.gk.ledger.payout
  com.gk.ledger.psp
  com.gk.ledger.account
  com.gk.ledger.settlement
  com.gk.ledger.reconcile
  com.gk.ledger.notify
  com.gk.ledger.audit
  com.gk.ledger.openapi
```

工程接入动作：

```text
1. 根 pom.xml 增加 <module>gk-ledger</module>
2. gk-admin/pom.xml 增加 gk-ledger 依赖
3. gk-ledger 复用 gk-infra、gk-common、gk-auth、gk-scheduler 能力
```

后台接口沿用现有项目风格：

```text
R.ok()
PageData
@RequestMap DynMap
@RequiresPermission
CrudServiceImpl
BaseDao
```

商户开放 API 单独放在：

```text
/openapi/v1/**
```

后台运营接口使用现有 JWT 和权限体系，商户开放 API 使用签名过滤器，不走后台 JWT。

## 7. 基础字段规范

所有业务表统一包含以下基础字段：

```sql
`id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
`remark` varchar(512) NULL DEFAULT NULL COMMENT '备注',
`created_by` bigint NULL DEFAULT NULL COMMENT '创建人ID',
`created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
`updated_by` bigint NULL DEFAULT NULL COMMENT '更新人ID',
`updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间'
```

金额字段统一使用：

```sql
decimal(20,6)
```

对外展示时按币种精度格式化，例如 PHP 展示两位小数。

## 8. 核心表分组

### 8.1 商户表

```text
merchant
merchant_app
merchant_fee_rate
merchant_settlement_config
```

`merchant` 记录支付商户主体。`merchant_app` 记录 API 接入应用和密钥。`merchant_fee_rate` 记录按国家、币种、支付方式、方向区分的费率。`merchant_settlement_config` 记录结算周期、延迟天数和留存比例。

### 8.2 支付方式与 PSP 表

```text
pay_method
psp_provider
psp_method
psp_route_rule
psp_request_log
psp_callback_log
```

商户只看到 `pay_method.method_code`。平台内部通过 `psp_route_rule` 找到可用的 `psp_method`，再由 PSP 适配器调用上游接口。

### 8.3 代收表

```text
pay_order
pay_order_payment_page
```

代收订单中 PSP 字段统一命名：

```text
psp_id
psp_code
psp_method_id
psp_method_code
psp_order_no
psp_pay_url
psp_request_json
psp_response_json
psp_callback_json
```

跳转页字段：

```text
cashier_url        平台自己的收银台 URL
psp_pay_url        PSP 返回的支付跳转 URL
qr_content         二维码内容
payment_data_json  PSP 返回的支付展示数据
page_return_url    商户前端同步返回地址
notify_url         商户服务器异步通知地址
expire_at          支付过期时间
```

如果 V1 先不做平台统一收银台，可以先返回 `psp_pay_url`。如果需要统一收银台，则使用 `pay_order_payment_page` 保存页面展示数据和 `page_token`。

### 8.4 代付表

```text
payout_order
payout_method_schema
```

`payout_order` 保留通用收款人字段：

```text
beneficiary_name
beneficiary_account
beneficiary_bank_code
beneficiary_mobile
beneficiary_email
beneficiary_id_type
beneficiary_id_no
extra_json
```

不同国家、不同支付方式的差异字段放入 `extra_json`，并通过 `payout_method_schema` 配置必填、类型、长度、正则和示例。这样后续接入越南、印尼、泰国或其他国家时，不需要频繁改订单主表。

### 8.5 账务表

```text
ledger_subject
ledger_account
ledger_journal
ledger_entry
```

`ledger_subject` 表示资金主体，例如商户、PSP、平台。`ledger_account` 表示某个资金主体在某个币种下的具体账户。`ledger_journal` 是一次业务事件的记账凭证头。`ledger_entry` 是该凭证下的借贷分录行。

### 8.6 结算表

```text
settle_release_plan
settle_batch
settle_batch_item
```

结算层不直接改余额，只负责决定哪些资金释放、哪些交易进入批次、报表如何汇总。真正余额变化仍然通过账务层完成。

### 8.7 对账、通知与审计表

```text
recon_file
recon_record
recon_exception
notify_merchant
audit_operation
```

V1 对账以文件上传为主。差错处理可以发起人工调账，但调账必须生成账务凭证和审计日志，不允许直接修改余额。

## 9. 多币种账务模型

账户按币种隔离。`ledger_account` 必须包含：

```text
subject_id
account_type
currency
balance
```

推荐唯一约束：

```text
uk_subject_type_currency(subject_id, account_type, currency)
```

同一个商户同时跑多个国家和币种时，会有多组账户：

```text
商户 A - PHP - 可用余额
商户 A - PHP - 代收冻结
商户 A - PHP - 代付冻结
商户 A - VND - 可用余额
商户 A - VND - 代收冻结
商户 A - VND - 代付冻结
```

V1 规则：

- 一张 `ledger_journal` 只允许一个币种。
- 同一币种下借方合计必须等于贷方合计。
- 不允许用 PHP 余额发起 VND 代付。
- 结算批次按 `merchant_id + country_code + currency + period` 生成。
- V1 不做跨币种换汇。

如果未来要做换汇，需要单独增加 FX 业务和汇率记录，不混入 V1 账务规则。

## 10. 账务分录规则

所有余额变化必须通过 `LedgerPostingService` 生成 `ledger_journal` 和 `ledger_entry`，业务代码不允许直接修改 `ledger_account.balance`。

### 10.1 代收成功

用户通过 PSP 支付成功后，商户资金先进入代收冻结余额。

```text
biz_type = PAY_SUCCESS
biz_no = pay_order.platform_order_no
```

分录：

```text
借：PLATFORM_IN_TRANSIT       订单金额
贷：MERCHANT_PAYIN_FROZEN     净入账金额
贷：PLATFORM_REVENUE          手续费
```

### 10.2 冻结释放

到释放时间后，将商户代收冻结余额释放到可用余额。

```text
biz_type = SETTLEMENT_RELEASE
biz_no = settle_release_plan.id 或 pay_order.platform_order_no
```

分录：

```text
借：MERCHANT_PAYIN_FROZEN
贷：MERCHANT_AVAILABLE
```

### 10.3 代付发起冻结

代付只允许使用商户可用余额，不垫资。发起时先冻结代付金额和手续费。

```text
biz_type = PAYOUT_FREEZE
biz_no = payout_order.platform_order_no
```

分录：

```text
借：MERCHANT_AVAILABLE
贷：MERCHANT_PAYOUT_FROZEN
```

金额为：

```text
payout_order.amount + payout_order.fee_amount
```

### 10.4 代付成功

PSP 确认出款成功后，确认商户冻结资金支出，并确认平台手续费收入。

```text
biz_type = PAYOUT_SUCCESS
biz_no = payout_order.platform_order_no
```

分录：

```text
借：MERCHANT_PAYOUT_FROZEN
贷：PSP_PAYABLE
贷：PLATFORM_REVENUE
```

### 10.5 代付失败

PSP 确认失败后，释放冻结资金回商户可用余额。

```text
biz_type = PAYOUT_FAILED_UNFREEZE
biz_no = payout_order.platform_order_no
```

分录：

```text
借：MERCHANT_PAYOUT_FROZEN
贷：MERCHANT_AVAILABLE
```

### 10.6 人工调账

差错处理产生人工调账时，不允许直接改余额，必须生成 `MANUAL_ADJUSTMENT` 类型凭证。

补给商户余额：

```text
借：PLATFORM_ADJUSTMENT_EXPENSE
贷：MERCHANT_AVAILABLE
```

扣减商户余额：

```text
借：MERCHANT_AVAILABLE
贷：PLATFORM_ADJUSTMENT_INCOME
```

人工调账必须填写原因、备注，并写入 `audit_operation`。

## 11. 订单状态机

### 11.1 代收订单状态

```text
CREATED       已创建，未提交 PSP
PROCESSING    已提交 PSP，等待用户支付或 PSP 回调
SUCCESS       支付成功，已记入商户代收冻结余额
FAILED        支付失败
EXPIRED       已过期
CLOSED        已关闭
```

代收成功后的动作：

```text
1. 更新 pay_order.status = SUCCESS
2. 写 ledger_journal / ledger_entry
3. 生成 settle_release_plan
4. 生成 notify_merchant
```

### 11.2 代付订单状态

```text
CREATED         已创建
BALANCE_FROZEN  已冻结商户可用余额
PROCESSING      已提交 PSP
SUCCESS         PSP 确认成功，冻结资金确认支出
FAILED          PSP 确认失败，冻结资金解冻
CANCELLED       未提交 PSP 前取消
```

订单进入终态后，不允许被重复回调或低优先级状态覆盖。

## 12. 商户开放 API

商户接口统一放在：

```text
/openapi/v1/**
```

每个请求必须带：

```text
app_id
timestamp
nonce
sign
```

签名规则：

```text
1. 获取所有请求参数
2. 去掉 null 和空字符串
3. 排除 sign 字段
4. 参数名按 ASCII 从小到大排序
5. 拼接 key=value&key2=value2
6. 末尾追加 &key=api_secret
7. MD5 后转小写
```

安全规则：

- `timestamp` 默认允许 5 分钟误差。
- `nonce` 在同一个 `app_id` 下 10 分钟内不可重复。
- IP 必须符合 `merchant_app.ip_whitelist` 配置。
- `app_id + merchant_order_no` 保证商户请求幂等。

接口：

```text
POST /openapi/v1/pay/orders
GET  /openapi/v1/pay/orders/{merchant_order_no}
POST /openapi/v1/payout/orders
GET  /openapi/v1/payout/orders/{merchant_order_no}
GET  /openapi/v1/accounts/balances
GET  /openapi/v1/settlements
GET  /openapi/v1/settlements/{batch_no}
```

商户通知也使用同一套签名规则。商户返回纯文本 `SUCCESS` 时视为通知成功，否则进入重试。

## 13. PSP 路由与适配器

商户只传平台统一 `method_code`。平台内部通过路由规则选择 PSP。

`psp_route_rule` 支持：

```text
tenant_id
merchant_id
app_id
country_code
currency
method_code
direction
psp_method_id
priority
weight
min_amount
max_amount
start_time
end_time
status
```

匹配优先级：

```text
1. app_id 精确规则
2. merchant_id 规则
3. tenant_id 规则
4. 平台全局规则
```

V1 先做规则筛选和固定优先级，不做智能动态调权。

PSP 适配器接口：

```java
public interface PspAdapter {
    String pspCode();

    PspPayResponse createPay(PspPayRequest request);

    PspPayoutResponse createPayout(PspPayoutRequest request);

    PspQueryResponse queryOrder(PspQueryRequest request);

    PspCallbackResult parseCallback(String body, Map<String, String> headers);
}
```

PSP 请求和回调必须记录到 `psp_request_log` 和 `psp_callback_log`，方便运营排查。

## 14. 结算设计

结算层回答：

- 哪些交易可以释放。
- 哪些资金进入本期结算批次。
- 本期应该给商户结算多少钱。
- 哪些订单、代付、手续费、调账纳入报表。
- 运营确认、导出、标记打款到哪一步。

代收成功后生成 `settle_release_plan`：

```text
tenant_id
merchant_id
app_id
source_biz_type = PAY_ORDER
source_biz_no
country_code
currency
amount
fee_amount
net_amount
release_at
status = PENDING
```

释放任务将到期计划从 `PENDING` 变更为 `RELEASED`，并请求账务层记账。

结算批次按以下维度生成：

```text
tenant_id + merchant_id + country_code + currency + period_start + period_end
```

结算批次状态：

```text
GENERATED
CONFIRMED
EXPORTED
PAID
CANCELLED
```

V1 轻审批模式下，高风险动作需要二次确认并写入 `audit_operation`。

## 15. 对账与差错处理

V1 只支持运营上传 PSP 对账文件。文件解析后进入 `recon_record`，系统按照以下顺序匹配：

```text
1. platform_order_no
2. psp_order_no
3. merchant_order_no + amount + trade_time 范围
```

匹配结果：

```text
MATCHED
AMOUNT_MISMATCH
STATUS_MISMATCH
MISSING_IN_PLATFORM
MISSING_IN_PSP
DUPLICATED
```

差错进入 `recon_exception`。差错状态：

```text
OPEN
PROCESSING
ADJUSTED
IGNORED
CLOSED
```

人工调账规则：

- 不能直接改 `pay_order`。
- 不能直接改 `ledger_account.balance`。
- 只能创建 `MANUAL_ADJUSTMENT` 类型的 `ledger_journal`。
- 必须填写 `reason` 和 `remark`。
- 必须写 `audit_operation`。

## 16. 后台运营功能

后台继续使用现有 JWT、权限、菜单体系。V1 菜单建议：

- 商户管理。
- 商户应用。
- 商户费率。
- 商户结算配置。
- 商户余额。
- 商户流水。
- PSP 管理。
- PSP 支付方式。
- PSP 路由规则。
- PSP 请求日志。
- PSP 回调日志。
- 代收订单。
- 代付订单。
- 资金主体。
- 账户余额。
- 记账凭证。
- 分录明细。
- 人工调账。
- 释放计划。
- 结算批次。
- 结算明细。
- 结算报表。
- 对账文件。
- 对账记录。
- 差错处理。

后台运营原则：

- 运营可以查订单、查日志、查分录、补发通知、处理差错。
- 运营不能绕过账务直接改余额。
- 运营不能随便修改订单终态。
- 高风险操作必须写审计。

## 17. 索引与幂等原则

交易表幂等：

```text
pay_order:
  uk_app_merchant_order(app_id, merchant_order_no)
  uk_platform_order_no(platform_order_no)

payout_order:
  uk_app_merchant_order(app_id, merchant_order_no)
  uk_platform_order_no(platform_order_no)
```

账务防重复：

```text
ledger_journal:
  uk_biz(biz_type, biz_no)
  uk_journal_no(journal_no)

ledger_account:
  uk_account_no(account_no)
  uk_subject_type_currency(subject_id, account_type, currency)
```

通知重试：

```text
notify_merchant:
  uk_notify_no(notify_no)
  idx_biz(biz_type, biz_no)
  idx_retry(status, next_retry_at)
```

PSP 日志：

```text
psp_request_log:
  uk_request_no(request_no)
  idx_biz(biz_type, biz_no)
  idx_psp_created(psp_id, created_at)

psp_callback_log:
  uk_callback_no(callback_no)
  idx_platform_order(platform_order_no)
  idx_psp_order(psp_id, psp_order_no)
```

## 18. 代码服务边界

`LedgerPostingService` 是账务唯一写入口，对外暴露：

```text
postPaySuccess(...)
postPayoutFreeze(...)
postPayoutSuccess(...)
postPayoutFailedUnfreeze(...)
postSettlementRelease(...)
postManualAdjustment(...)
```

`PayOrderService` 负责代收订单生命周期。`PayoutOrderService` 负责代付订单生命周期。`PspRouteService` 负责路由。`PspAdapterRegistry` 负责找到对应 PSP 适配器。`MerchantSignatureFilter` 负责开放 API 签名、nonce、防重放和上下文注入。

关键事务边界：

- 代收成功：更新订单、写账务、生成释放计划、生成通知在一个短事务内完成。
- 代付冻结：创建订单、冻结余额、写冻结分录在一个事务内完成。
- PSP HTTP 调用不放在长事务中。
- PSP 回调处理必须做订单终态保护和账务幂等。

## 19. 落地里程碑

### 阶段 0：工程接入

- 根 `pom.xml` 加入 `gk-ledger`。
- `gk-admin/pom.xml` 依赖 `gk-ledger`。
- 建立 `gk-ledger` 标准包结构。
- 准备第一版 SQL 脚本。

### 阶段 1：商户与配置

- 实现商户、商户应用、费率、结算配置 CRUD。
- 实现支付方式、PSP、PSP 支付方式、路由规则 CRUD。

### 阶段 2：账务核心

- 实现资金主体、账户、凭证、分录。
- 实现 `LedgerPostingService`。
- 验证借贷平衡、币种隔离、余额不足拒绝、防重复记账。

### 阶段 3：代收闭环

- 实现商户签名过滤器。
- 实现代收创建和查询。
- 实现 mock PSP。
- 跑通代收成功、记账冻结、释放计划、商户通知。

### 阶段 4：代付闭环

- 实现代付创建和查询。
- 实现代付字段 schema 校验。
- 跑通余额校验、冻结、PSP 提交、成功确认支出、失败解冻。

### 阶段 5：结算

- 实现释放计划任务。
- 实现结算批次和明细。
- 实现 CSV 报表导出。

### 阶段 6：对账与差错

- 实现对账文件上传、解析、匹配。
- 实现差错单和人工调账。

### 阶段 7：通知、日志、审计

- 实现商户通知重试。
- 补齐 PSP 请求日志、PSP 回调日志。
- 高风险操作写入审计。

## 20. 验收标准

最小可演示闭环：

```text
商户创建
商户应用创建
商户密钥签名调用
创建代收订单
PSP 路由
PSP 回调成功
代收金额入商户冻结余额
释放到商户可用余额
查询多币种余额
生成结算批次
导出结算报表
商户通知成功
```

代付闭环：

```text
创建代付订单
动态校验收款字段
校验商户可用余额
冻结余额
提交 PSP
成功确认支出
失败解冻
商户通知
```

不能破坏的规则：

- 不能直接改 `ledger_account.balance`。
- 不能绕过 `ledger_journal` 和 `ledger_entry` 记账。
- 同一 `biz_type + biz_no` 只能记账一次。
- 同一 `app_id + merchant_order_no` 只能创建一笔订单。
- 不同币种账户不能混用。
- 代付余额不足必须拒绝。
- 订单终态不能被重复回调覆盖。
- 人工调账必须留原因、备注和审计记录。

优先测试：

- 签名正确、错误、空值、排序、排除 `sign`。
- nonce 重放拦截。
- 代收重复下单幂等。
- 代收成功重复回调幂等。
- 账务借贷平衡。
- PHP/VND 多币种余额隔离。
- 代付余额不足。
- 代付成功扣款。
- 代付失败解冻。
- 释放计划重复执行不重复入账。
- 结算批次重复生成不重复纳入明细。

