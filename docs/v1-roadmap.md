我这次按代码现状重新梳理了一遍，不只看旧文档。整体判断：**后台基础、商户、订单、账务、PSP 路由、回调、商户通知这些骨架已经比较完整；但第一版真正上线还差“真实 PSP 调用 + OpenAPI 安全 + 补偿任务 + 状态轨迹/隔离”这几个关键闭环。**

**当前业务主链路**
代收链路现在是：

商户调用 `/api/v1/pay/create` → 创建 `pay_order` → 算商户手续费 → 选 PSP 路由 → 调 PSP 适配器 → 更新订单为处理中 → PSP 回调 → 验签/幂等/金额校验 → 订单成功 → 账务入账 → 创建商户通知任务 → 定时任务发通知。

主要入口在 [OpenApiV1Controller.java](E:/IdeaProjects/gk-union/gk-ledger/src/main/java/com/gk/openapi/controller/OpenApiV1Controller.java:62)，订单编排在 [OpenPayOrderServiceImpl.java](E:/IdeaProjects/gk-union/gk-ledger/src/main/java/com/gk/openapi/service/impl/OpenPayOrderServiceImpl.java:52)，回调闭环在 [PspCallbackService.java](E:/IdeaProjects/gk-union/gk-ledger/src/main/java/com/gk/psp/callback/PspCallbackService.java:51)。

代付链路现在是：

商户调用 `/api/v1/payout/create` → 创建 `payout_order` → 算手续费 → 冻结商户可用余额 → 选 PSP 路由 → 调 PSP 适配器 → PSP 回调成功则扣冻结，失败则解冻 → 创建商户通知任务。

入口在 [OpenPayoutOrderServiceImpl.java](E:/IdeaProjects/gk-union/gk-ledger/src/main/java/com/gk/openapi/service/impl/OpenPayoutOrderServiceImpl.java:59)，冻结账务已经接到 [LedgerPostingServiceImpl.java](E:/IdeaProjects/gk-union/gk-ledger/src/main/java/com/gk/ledger/service/impl/LedgerPostingServiceImpl.java:103)。

**已经完成的逻辑功能**
- 商户管理：商户创建、商户号生成、默认结算参数、创建时自动开账。
  位置：[MerchantServiceImpl.java](E:/IdeaProjects/gk-union/gk-ledger/src/main/java/com/gk/merchant/service/impl/MerchantServiceImpl.java:63)

- 商户应用：`app_id/api_secret` 自动生成、密钥重置、列表脱敏。
  位置：[MerchantAppServiceImpl.java](E:/IdeaProjects/gk-union/gk-ledger/src/main/java/com/gk/merchant/service/impl/MerchantAppServiceImpl.java:71)

- OpenAPI 基础接口：余额查询、支付方式查询、代收创建/查询、代付创建/查询。
  位置：[OpenApiV1Controller.java](E:/IdeaProjects/gk-union/gk-ledger/src/main/java/com/gk/openapi/controller/OpenApiV1Controller.java:43)

- PSP 基础配置：provider、method、account、route rule、fee rule、request log、callback log 的表、CRUD 和后台接口基本都有。

- PSP 路由：按租户、商户、应用、国家、币种、支付方式、方向、金额、优先级选路由。
  位置：[PspRouteSelectorImpl.java](E:/IdeaProjects/gk-union/gk-ledger/src/main/java/com/gk/psp/route/impl/PspRouteSelectorImpl.java:1)

- 账务核心：账户、余额、凭证、分录、冻结明细已经建模；代收入账、代付冻结、代付成功、代付失败解冻都有实现。
  位置：[LedgerPostingServiceImpl.java](E:/IdeaProjects/gk-union/gk-ledger/src/main/java/com/gk/ledger/service/impl/LedgerPostingServiceImpl.java:57)

- PSP 回调处理：回调日志、解析、验签、订单状态更新、终态校验、账务过账、创建商户通知任务已经串起来。
  位置：[PspCallbackService.java](E:/IdeaProjects/gk-union/gk-ledger/src/main/java/com/gk/psp/callback/PspCallbackService.java:66)

- 商户异步通知：通知任务、通知记录、HTTP POST、签名、重试、死信、手动重发已经有了。
  位置：[MerchantNotifyExecutor.java](E:/IdeaProjects/gk-union/gk-ledger/src/main/java/com/gk/payment/notify/MerchantNotifyExecutor.java:42)，Quartz 种子在 [v1_merchant_notify_job.sql](E:/IdeaProjects/gk-union/gk-ledger/src/main/resources/sql/v1_merchant_notify_job.sql:9)

- 后台查询：商户、商户应用、PSP 配置、订单、账务、通知任务/记录、请求日志等基础后台接口已经铺开。

**还差的关键功能**
- 真实 PSP HTTP 调用还没完成。`WorldPspSubmitAdapter` 现在仍是写死返回，不是真的请求上游。
  位置：[WorldPspSubmitAdapter.java](E:/IdeaProjects/gk-union/gk-ledger/src/main/java/com/gk/psp/adapter/world/WorldPspSubmitAdapter.java:30)

- OpenAPI 鉴权没真正打开。时间戳校验、签名校验被注释，nonce 为空也直接放过。
  位置：[OpenApiAuthFilter.java](E:/IdeaProjects/gk-union/gk-ledger/src/main/java/com/gk/openapi/security/OpenApiAuthFilter.java:123)

- PSP 查单能力缺失。当前 PSP adapter 只有创建代收/代付，没有查询订单状态，所以无法做处理中补单。
  相关接口：[PspPayAdapter.java](E:/IdeaProjects/gk-union/gk-ledger/src/main/java/com/gk/psp/adapter/PspPayAdapter.java:10)

- 业务补偿定时任务不足。目前只有商户通知定时任务；还缺代收超时关闭、代收/代付处理中查单、代付异常解冻补偿。

- 订单状态轨迹没写入。`OrderStatusLog` 有表和 CRUD，但主链路没有真正记录状态变更。
  位置：[OrderStatusLogServiceImpl.java](E:/IdeaProjects/gk-union/gk-ledger/src/main/java/com/gk/payment/service/impl/OrderStatusLogServiceImpl.java:14)

- 结算模型口径不一致。表里有 `settle_status=PENDING`，但代收成功账务现在直接入商户可用余额。要决定 V1 是“成功即入可用”，还是严格做“待结算 → 可用释放”。

- 多租户后台数据隔离还没完全落地。`@DataScope` 基础设施有，但业务查询基本没挂载，存在后台越权查询风险。

- Outbox 只有 SQL，没有 Java producer/consumer/重试机制。V1 可以先用商户通知表顶住，但如果要事件驱动补偿，就要补。
  位置：[v1_mq_schema.sql](E:/IdeaProjects/gk-union/gk-ledger/src/main/resources/sql/v1_mq_schema.sql:56)

**后续计划**
P0，先打通可演示闭环：

1. 打开 OpenAPI 安全：恢复时间戳校验、签名校验，nonce 改成必填。
2. 接入一个真实 PSP 沙箱：把 `WorldPspSubmitAdapter` 改成真实 HTTP 下单。
3. 给 PSP adapter 增加查单接口：代收查单、代付查单。
4. 做处理中补单任务：扫 `PROCESSING` 订单，查 PSP，按结果走回调同样的账务/通知逻辑。
5. 跑通验收：一笔代收成功入账并通知商户；一笔代付冻结、成功扣款或失败解冻并通知商户。

P1，补可追溯和可补偿：

1. 记录 `order_status_log`，所有 CREATED/PROCESSING/SUCCESS/FAILED/CLOSED 都要有轨迹。
2. 做代收超时关闭任务。
3. 做代付提交失败/长时间处理中补偿。
4. 明确结算口径；如果要 T+N，就补待结算账户和释放任务。

P2，补上线安全：

1. 给后台 list/page 挂 `@DataScope`，强制 tenant/merchant 隔离。
2. 商户密钥、PSP 密钥加密存储或至少环境密钥加密。
3. 完善 request/response 脱敏，避免日志里出现密钥、银行卡、手机号。
4. 补核心集成测试：代收成功、重复回调、金额不一致、代付成功、代付失败解冻、通知重试。

P3，再做增强：

1. Outbox Java 层实现。
2. 自动对账和差错处理。
3. 商户门户或商户自助查询。
4. 更复杂的 PSP 路由和风控策略。

我建议你下一步就从 **P0-1 OpenAPI 鉴权恢复** 和 **P0-2 真实 PSP 下单/查单接口** 开始。现在项目的骨架已经够用了，真正缺的是把“模拟链路”换成“真实资金链路”。