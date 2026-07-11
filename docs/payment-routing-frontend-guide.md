# 支付路由与支付方案前端开发说明

本文档给前端开发和前端 AI 使用，目标是开发“支付配置 / 支付方案”页面。

核心原则：

```text
payment_route_* 是唯一配置源
payment_plan_* 是唯一运行时发布结果
```

不要让前端把 `payment_plan_*` 当成可编辑配置。前端只编辑 `payment_route_rule`、`payment_route_group`、`payment_route_channel` 等源配置，然后通过 `preview` 预览，通过 `publish` 发布成运行时使用的 ACTIVE payment plan。

## 1. 数据关系

```text
merchant_fee_rule
  决定商户手续费

psp_fee_rule
  决定 PSP 成本手续费

payment_route_rule
  路由规则：商户/应用/方向/国家/币种/支付方式/金额 -> route_group

payment_route_group
  路由组/通道池：定义一组 PSP 候选通道的业务维度

payment_route_channel
  路由通道候选：group 下绑定 psp_provider + psp_method + psp_account + 可选 psp_fee_rule

payment_plan_catalog
  发布版本头

payment_plan_bucket
  发布版本的金额段快照

payment_plan_route_option
  发布版本的 PSP 候选通道快照
```

关系：

```text
payment_route_rule 多对一 payment_route_group
payment_route_group 一对多 payment_route_channel

payment_plan_catalog 一对多 payment_plan_bucket
payment_plan_bucket 一对多 payment_plan_route_option
```

## 2. 前端页面建议

菜单建议：

```text
支付配置
  支付方式
  商户费率规则
  PSP成本规则
  支付路由组
  支付路由规则
  支付方案
```

### 2.1 支付路由组页面

接口：

```text
GET    /payment/route-group/page
GET    /payment/route-group/{id}
GET    /payment/route-group/{id}/check
POST   /payment/route-group
PUT    /payment/route-group/{id}
DELETE /payment/route-group?ids=1&ids=2
```

列表字段：

```text
groupCode        路由组编码
groupName        路由组名称
direction        PAYIN/PAYOUT
countryCode      国家/地区，可为空字符串
currency         币种
methodCode       支付方式
strategy         组内策略
status           状态
remark           备注
```

新增/编辑请求：

```json
{
  "tenantId": 1,
  "groupCode": "PH_GCASH_PAYIN",
  "groupName": "菲律宾GCASH代收池",
  "direction": "PAYIN",
  "countryCode": "",
  "currency": "PHP",
  "methodCode": "GCASH",
  "strategy": "PRIORITY_WEIGHT",
  "status": 1,
  "remark": "GCASH代收默认通道池"
}
```

前端规则：

```text
countryCode 可以为空字符串，表示通用规则
currency 必填
methodCode 必填
direction 必填
status 使用 1 正常、2 暂停、3 停用
```

金额范围统一约定：

```text
minAmount 为空或 0：从 0 开始
maxAmount 为空或 0：不限制最大金额
前端文案建议显示“最大金额填 0 表示不限”
```

检测按钮：

```text
GET /payment/route-group/{id}/check
```

用途：检测引用该路由组的 active route_rule 金额区间，是否被 active route_channel、psp_method、已绑定的 psp_fee_rule 完整覆盖。

前端展示建议：

```text
valid=false 时显示红色风险
routeRules[].gaps 显示未覆盖金额段
routeRules[].coveredRanges 显示已覆盖金额段
routeRules[].channels 显示每个 PSP 通道实际可用金额段
```

### 2.2 支付路由组详情 / 通道候选页面

建议从路由组列表进入详情页，在详情页展示该 group 下的 `payment_route_channel`。

接口：

```text
GET    /payment/route-channel/page?groupId=xxx
GET    /payment/route-channel/{id}
POST   /payment/route-channel
PUT    /payment/route-channel/{id}
DELETE /payment/route-channel?ids=1&ids=2
```

列表字段：

```text
groupId
pspId
pspMethodId
pspAccountId
pspFeeRuleId
priority
weight
fallbackOrder
minAmount
maxAmount
status
remark
```

新增/编辑请求：

```json
{
  "groupId": 100,
  "pspId": 200,
  "pspMethodId": 300,
  "pspAccountId": 400,
  "pspFeeRuleId": 500,
  "priority": 1,
  "weight": 100,
  "fallbackOrder": 1,
  "minAmount": 0,
  "maxAmount": 100000,
  "status": 1,
  "remark": "XPay GCASH 主通道"
}
```

前端规则：

```text
pspFeeRuleId 可为空
如果为空，发布预览时后端会自动匹配 psp_fee_rule
如果有值，后端会校验 PSP、账户、方法、币种、方向是否匹配
priority 越小越优先
weight 用于同优先级权重分流
fallbackOrder 用于失败后的备用顺序
```

级联下拉选项：

```text
GET /payment/route-channel/options?groupId=100

或新增路由组未保存时：

GET /payment/route-channel/options?tenantId=1&direction=PAYIN&countryCode=PH&currency=PHP&methodCode=GCASH
```

该接口会根据已保存路由组，或直接传入的 `tenantId / direction / countryCode / currency / methodCode` 一次性返回：

```text
routeGroup
pspProviders
pspMethods
pspAccounts
pspFeeRules
```

前端拿到后在本地做级联过滤：

```text
选择 PSP 后：按 pspId 过滤 pspMethods
选择 PSP 方法后：按 pspId 过滤 pspAccounts
选择 PSP 账户后：按 pspId、pspMethodId、pspAccountId 过滤 pspFeeRules
```

也可以复用这些基础接口做远程搜索：

```text
pspId           GET /psp/provider/page
pspMethodId     GET /psp/method/page?pspId=xxx
pspAccountId    GET /psp/account/page?pspId=xxx
pspFeeRuleId    GET /psp/fee-rule/page?pspId=xxx&pspMethodId=xxx&pspAccountId=xxx
```

### 2.3 支付路由规则页面

接口：

```text
GET    /payment/route-rule/page
GET    /payment/route-rule/{id}
POST   /payment/route-rule
PUT    /payment/route-rule/{id}
DELETE /payment/route-rule?ids=1&ids=2
```

列表字段：

```text
ruleName
merchantId
merchantAppId
direction
countryCode
currency
methodCode
minAmount
maxAmount
groupId
priority
effectiveAt
expireAt
status
remark
```

新增/编辑请求：

```json
{
  "tenantId": 1,
  "ruleName": "商户A GCASH代收路由",
  "merchantId": 10,
  "merchantAppId": 20,
  "direction": "PAYIN",
  "countryCode": "",
  "currency": "PHP",
  "methodCode": "GCASH",
  "minAmount": 0,
  "maxAmount": 100000,
  "groupId": 100,
  "priority": 1,
  "effectiveAt": null,
  "expireAt": null,
  "status": 1,
  "remark": "商户A GCASH默认路由"
}
```

前端规则：

```text
route_rule 不直接绑定 PSP
route_rule 只绑定 groupId
merchantId 可为空，表示租户级默认规则
merchantAppId 可为空，表示不区分应用
countryCode 可为空字符串，表示通用规则
金额区间建议第一阶段和 merchant_fee_rule 保持一致，方便 preview 展示
```

### 2.4 支付方案页面

这个页面管理 `payment_plan_*`，但不直接编辑 `payment_plan_*`。

接口：

```text
GET  /payment/payment-plan/page
GET  /payment/payment-plan/{id}
POST /payment/payment-plan/preview
POST /payment/payment-plan/publish
POST /payment/payment-plan/{id}/activate
POST /payment/payment-plan/{id}/retire
```

版本列表查询参数：

```text
page
limit
tenantId
merchantId
merchantAppId
direction
countryCode
currency
methodCode
status
```

版本列表返回：

```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "total": 1,
    "items": [
      {
        "id": 1000,
        "tenantId": 1,
        "merchantId": 10,
        "merchantAppId": 20,
        "direction": "PAYIN",
        "countryCode": "",
        "currency": "PHP",
        "methodCode": "GCASH",
        "version": 3,
        "status": "ACTIVE",
        "bucketCount": 2,
        "routeOptionCount": 4,
        "compiledAt": "2026-06-24T10:00:00Z",
        "activatedAt": "2026-06-24T10:01:00Z",
        "remark": "正式版本"
      }
    ]
  }
}
```

详情返回结构：

```text
catalog
  版本头

buckets[]
  金额段
  merchantFeeSnapshotJson
  routeOptions[]
    PSP候选
    pspFeeSnapshotJson
    routeRuleSnapshotJson
    routeGroupSnapshotJson
    routeChannelSnapshotJson
    pspProviderSnapshotJson
    pspMethodSnapshotJson
    pspAccountSnapshotJson
```

预览请求：

```json
{
  "tenantId": 1,
  "merchantId": 10,
  "merchantAppId": 20,
  "merchantFeeRuleId": null,
  "direction": "PAYIN",
  "countryCode": "",
  "currency": "PHP",
  "methodCode": "GCASH",
  "minAmount": 0,
  "maxAmount": 100000,
  "pspFeeRequired": false,
  "testCases": [
    {
      "merchantOrderId": "TEST001",
      "amount": 100,
      "payee": {
        "bankCode": ""
      }
    }
  ]
}
```

预览返回重点字段：

```text
valid
requestInfo
warnings[]
errors[]
merchantFeeRules[]
routeRules[]
routeGroups[]
routeChannels[]
pspMethods[]
pspFeeRules[]
routeOptions[]
buckets[]
  startAmount
  endAmount
  amountRangeText
  merchantFeeRule
    ruleName
    feeMode
    feeRate
    feeFixed
    feeBearer
  routeOptions[]
    route
      routeName
    routeGroup
      groupName
    routeChannel
      priority
      weight
      fallbackOrder
      pspFeeRuleId
    psp
      pspCode
      pspName
    pspMethod
      methodName
      pspMethodCode
    pspAccount
      pspAccountNo
    pspFeeRule
      ruleName
      feeMode
      feeRate
      feeFixed
```

预览逻辑说明：

```text
preview 不落库，只从 merchant_fee_rule、payment_route_rule、payment_route_group、payment_route_channel、PSP资源表实时编译诊断。
即使 merchant_fee_rule 缺失，也会继续诊断 payment_route_* 路由链路，方便一次性看到所有配置问题。
valid=false 时，前端仍然要展示 requestInfo、routeRules、routeGroups、routeChannels、routeOptions、warnings、errors。
publish 比 preview 更严格，只有 valid=true 的编译结果才允许写入 payment_plan_* 并激活。
```

批量预览受影响商户：

```text
POST /payment/payment-plan/batch-preview-by-route-group
```

使用场景：

```text
在路由组中新增/修改 PSP 通道后，前端点击“批量预览”，后端按 routeGroupId 找到引用该路由组的 payment_route_rule，
并对每个明确商户的规则内部调用 PaymentPlanCompiler.compile，不通过 HTTP 再调用 preview 接口。
merchantId 为空的通用规则不会自动展开全平台商户，会返回 skipped=true 和 warning，要求人工选择商户范围。
```

请求：

```json
{
  "tenantId": 1,
  "routeGroupId": 100,
  "defaultMinAmount": 0,
  "defaultMaxAmount": 100000,
  "pspFeeRequired": false
}
```

返回重点字段：

```text
routeGroupId
groupCode
groupName
total
validCount
invalidCount
skippedCount
items[]
  routeRuleId
  routeRuleName
  merchantId
  merchantAppId
  direction
  countryCode
  currency
  methodCode
  minAmount
  maxAmount
  valid
  skipped
  skipReason
  bucketCount
  routeOptionCount
  warningCount
  errorCount
  warnings[]
  errors[]
  preview
```

发布请求和预览请求基本一致：

```json
{
  "tenantId": 1,
  "merchantId": 10,
  "merchantAppId": 20,
  "merchantFeeRuleId": null,
  "direction": "PAYIN",
  "countryCode": "",
  "currency": "PHP",
  "methodCode": "GCASH",
  "minAmount": 0,
  "maxAmount": 100000,
  "pspFeeRequired": false,
  "remark": "正式发布"
}
```

发布返回：

```json
{
  "published": true,
  "catalogId": 1000,
  "versionNo": 3,
  "status": "ACTIVE",
  "bucketCount": 2,
  "routeOptionCount": 4,
  "redisEvicted": true
}
```

前端按钮规则：

```text
preview       不落库，只展示预览结果
publish       编译并发布为 ACTIVE
activate      把历史版本重新激活，同维度旧 ACTIVE 自动 RETIRED
retire        停用某版本
```

## 3. 支撑配置接口

商户费率：

```text
GET    /payment/merchant-fee-rule/page
GET    /payment/merchant-fee-rule/{id}
POST   /payment/merchant-fee-rule
PUT    /payment/merchant-fee-rule/{id}
DELETE /payment/merchant-fee-rule
```

PSP成本规则：

```text
GET    /psp/fee-rule/page
GET    /psp/fee-rule/{id}
POST   /psp/fee-rule
PUT    /psp/fee-rule/{id}
DELETE /psp/fee-rule
```

PSP资源：

```text
GET /psp/provider/page
GET /psp/method/page
GET /psp/method/dict-psp
GET /psp/method/dict
GET /psp/method/fee-rule-dict
GET /psp/account/page
GET /psp/account/dict
```

支付方式：

```text
GET /payment/method/page
GET /payment/method/options
GET /payment/method/dict
GET /payment/method/label-dict
```

银行卡代付映射：

```text
GET /psp/bank-mapping/page
GET /psp/bank-mapping/matrix
PUT /psp/bank-mapping/matrix
```

## 4. 通用响应结构

普通响应：

```json
{
  "code": 0,
  "msg": "success",
  "data": {}
}
```

分页响应：

```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "total": 100,
    "items": []
  }
}
```

分页请求通用参数：

```text
page        页码，从 1 开始
limit       每页条数
orderField 选填
order      asc/desc
```

## 5. 前端交互流程

推荐完整流程：

```text
1. 配置商户费率规则 merchant_fee_rule
2. 配置 PSP 成本规则 psp_fee_rule
3. 配置支付路由组 payment_route_group
4. 在路由组详情里配置通道 payment_route_channel
5. 配置支付路由规则 payment_route_rule
6. 在支付方案页面选择商户、应用、方向、币种、方式、金额范围
7. 点击预览 preview
8. 前端展示金额段、商户费率、路由规则、路由组、PSP通道、PSP成本、warning/error
9. valid=true 时允许发布 publish
10. 发布后在版本列表展示 ACTIVE 版本
```

## 6. 页面展示重点

支付方案预览页建议用表格展示：

```text
金额段
商户费率名称
商户费率类型
商户费率比例
商户固定手续费
路由规则名称
路由组名称
PSP名称
PSP方法
PSP账户
PSP成本规则名称
PSP成本比例
PSP固定成本
优先级
权重
备用顺序
状态
```

warning/error 展示：

```text
errors 必须红色展示，并阻止发布
warnings 黄色展示，可以发布，但需要用户确认
```

常见 warning：

```text
PSP_FEE_RULE_MISSING
PSP_FEE_RULE_INVALID
PSP_RESOURCE_UNAVAILABLE
PSP_BANK_MAPPING_MISSING
PAYMENT_ROUTE_CHANNEL_INVALID
```

## 7. 给前端 AI 的开发提示词

可以把下面这段直接交给前端 AI：

```text
你是一个资深 Vue/React 后台管理系统前端工程师。请基于当前后台项目风格，开发支付配置与支付方案页面。

核心原则：
1. payment_route_* 是唯一配置源。
2. payment_plan_* 是唯一运行时发布结果。
3. 前端不要直接编辑 payment_plan_*，只能查看版本、查看详情、激活、停用。
4. 前端编辑 payment_route_group、payment_route_channel、payment_route_rule。
5. 支付方案通过 preview 预览，通过 publish 发布。

需要开发的页面：

一、支付路由组页面
- 列表、搜索、新增、编辑、删除。
- 字段：groupCode、groupName、direction、countryCode、currency、methodCode、strategy、status、remark。
- 从路由组进入详情页，详情页管理 payment_route_channel。

二、路由通道候选页面
- 按 groupId 查询。
- 字段：pspId、pspMethodId、pspAccountId、pspFeeRuleId、priority、weight、fallbackOrder、minAmount、maxAmount、status、remark。
- pspFeeRuleId 可为空。
- 下拉数据优先使用 GET /payment/route-channel/options?groupId=xxx 一次性获取；新增路由组未保存时，使用 tenantId、direction、countryCode、currency、methodCode 查询。
- 下拉依赖：先选 pspId，再选 pspMethodId，再选 pspAccountId，再选 pspFeeRuleId，前端基于接口返回数组本地过滤。

三、支付路由规则页面
- 列表、搜索、新增、编辑、删除。
- 字段：ruleName、merchantId、merchantAppId、direction、countryCode、currency、methodCode、minAmount、maxAmount、groupId、priority、effectiveAt、expireAt、status、remark。
- groupId 从 payment_route_group 下拉选择。
- route_rule 不直接选择 PSP。

四、支付方案页面
- 顶部筛选：tenantId、merchantId、merchantAppId、direction、countryCode、currency、methodCode、minAmount、maxAmount、pspFeeRequired。
- 按钮：预览、发布。
- 预览结果展示 buckets，每个 bucket 展示 merchantFeeRule 和 routeOptions。
- routeOptions 展示 route、routeGroup、routeChannel、psp、pspMethod、pspAccount、pspFeeRule。
- errors 红色显示并禁止发布。
- warnings 黄色显示，允许发布但要二次确认。

五、支付方案版本页面
- 使用 GET /payment/payment-plan/page 查询版本。
- 使用 GET /payment/payment-plan/{id} 查看详情。
- 支持 POST /payment/payment-plan/{id}/activate 激活版本。
- 支持 POST /payment/payment-plan/{id}/retire 停用版本。
- ACTIVE 用绿色标签，RETIRED 用灰色标签，STAGING 用黄色标签。

接口响应结构统一为：
{
  code: 0,
  msg: "success",
  data: ...
}

分页响应：
{
  total: number,
  items: []
}

注意：
- countryCode 允许为空字符串，表示通用规则。
- direction 只允许 PAYIN/PAYOUT。
- status 使用 1 正常、2 暂停、3 停用。
- payment_plan_* 只读展示，不允许编辑 bucket 或 routeOption。
- 发布后运行时只读 ACTIVE payment_plan。
```

## 8. 开发顺序建议

```text
1. 先做支付方案版本列表和详情
2. 再做支付方案 preview/publish
3. 再做 payment_route_group
4. 再做 group 详情里的 payment_route_channel
5. 再做 payment_route_rule
6. 最后把 merchant_fee_rule、psp_fee_rule 的选择器串起来
```

原因：

```text
payment_plan 页面最能验证后端编译结果
route_group/channel/rule 页面只是源配置 CRUD
先把预览页面打通，能最快发现配置缺口
```
