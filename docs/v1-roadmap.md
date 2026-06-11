# 国际四方 V1 实施计划（按优先级）

> 目标（V1 验收标准）：**能稳定完成一笔代收、一笔代付，钱账正确，可追溯，可补偿**。
> 验收的关键不是功能多，而是每一笔钱从订单 → PSP → 账务 → 余额 → 通知都能闭环追踪。

本文档基于对当前代码库的盘点，给出"接下来要做什么"的优先级路线图。

---

## 一、现状结论

项目的"骨架和账务内核"已经比较扎实，但**主链路目前是"假的"——尚未跑通真实资金闭环**。

### 已经做得不错（DONE）

| 领域 | 说明 |
|------|------|
| IAM | 用户/主体/角色/部门/租户、登录 JWT、`@PreAuthorize` + 超管绕过、租户开通 `onboard()` |
| 账务内核 | `LedgerPostingServiceImpl` 真正的复式记账（journal→entry→balance，悲观锁 + 幂等）；代收入账、代付冻结/成功/解冻 |
| 订单编排 | `OpenPayOrderServiceImpl` / `OpenPayoutOrderServiceImpl`（幂等、算费、路由、派发）、PSP 路由选择、回调解析（World 验签） |
| 日志 | PSP 请求日志、商户请求日志、Telegram 机器人入站 |

### 致命缺口（卡住 V1 闭环）

1. **没有真实 PSP HTTP 调用** —— 所有 submit 适配器（demo/world）都返回写死的假单号，钱根本没动。
2. **开放 API 签名/时间戳校验被注释掉**（`OpenApiAuthFilter` 124-127 行）—— 等于没鉴权。
3. **商户通知只建了 task，没有发送器/重试** —— 商户永远收不到回调，闭环断裂。
4. **没有任何业务定时任务** —— 补单查询、超时关单、通知重试、结算释放全缺（`gk-scheduler` 只有 `testTask`）。
5. **结算生命周期不流转** —— `settleStatus`/`settleAt` 字段有，但没人改（待结算永远到不了可用）。
6. **商户 app 没有 app_id/api_secret 生成逻辑** —— 商户没法接入。
7. **开户没自动建 ledger_account** —— 下单时账户可能不存在。
8. **`@DataScope` 写好了却没挂到任何方法上** —— 多租户后台查询存在越权风险。
9. **Outbox 只有建表 SQL，没 Java 实现。**

---

## 二、计划（按优先级）

### 🔴 P0 —— 打通"一笔真实资金闭环"（不做无法上线）

> 验收：用真实 PSP 沙箱，商户签名调一笔代收 → 成功回调 → 入账 → 商户收到带签名的通知；代付同理。

| # | 任务 | 关键改动点 |
|---|------|-----------|
| P0-1 | **接入 1 个真实 PSP（HTTP 落地）** | 把 `WorldPspSubmitAdapter` 的写死返回换成真实 `RestClient` 调用（参考 `TgBotApiClient` 写法），打通下单/查单/代付；补 `DemoPspCallbackAdapter` 让 demo 链路可本地端到端自测 |
| P0-2 | **重新开启开放 API 鉴权** | 放开 `OpenApiAuthFilter` 的 `validateTimestamp()` + `validateSortedParamSignature()`，nonce 改为强制 |
| P0-3 | **商户 app 凭证发放** | `MerchantAppServiceImpl` 保存时生成 `app_id`/`api_secret`、密钥加密存储、提供重置/轮换接口 |
| P0-4 | **开户自动建账** | 商户 onboarding/创建时自动 `ledger_account` 开户（代收待结算、可用、冻结等账户） |
| P0-5 | **商户通知发送器（闭环关键）** | `gk-scheduler` 加 worker：扫 `merchant_notify_task` 的 `INIT` → HMAC 签名 → HTTP POST → 写 `merchant_notify_record` → 失败退避重试；后台支持手动重发 |

### 🟠 P1 —— 可补偿 / 可追溯（V1 验收硬指标）

| # | 任务 | 关键改动点 |
|---|------|-----------|
| P1-1 | **业务定时任务** | 基于现成 Quartz 框架建 `ITask` 实现 + 种子 job：代收超时关单、代付/代收 PROCESSING 状态补查询、通知重试 |
| P1-2 | **结算释放** | `settleStatus` 流转 + 待结算→可用的 ledger 过账（T+N） |
| P1-3 | **订单状态轨迹** | 在代收/代付流程中真正写 `OrderStatusLogEntity`（目前是空壳） |

### 🟡 P2 —— 多租户安全加固（SaaS 必须，但不卡资金链路）

| # | 任务 | 关键改动点 |
|---|------|-----------|
| P2-1 | **挂载 `@DataScope`** | 加到后台 list/page 方法 + MyBatis XML 加 `${sqlFilter}`，强制 tenant_id/merchant_id 隔离 |
| P2-2 | **配置安全** | JWT 密钥、PSP 密钥、加密 key 外置到环境变量（现在 `JwtUtils` 硬编码） |
| P2-3 | **Outbox 机制（可选）** | V1 也可先用通知表代替；如需要再补 producer/consumer/重试 Java 层 |

### ⚪ P3 —— 完善与体验（后续迭代）

- 自动对账 / 差错处理
- 多主体登录切换
- `GET /sys/role/{id}` 与 user `getById` 补全字段
- `SysI18nController` 补 `@PreAuthorize`
- `gk-cloak` 风控
- Telegram 出站队列发送器
- 集成测试覆盖主链路、关键接口可观测性（日志 / 指标）

---

## 三、执行顺序（里程碑）

| 里程碑 | 内容 | 达成标志 |
|--------|------|---------|
| **M1（闭环可演示）** | P0 全部 | 真实跑通一笔代收 + 代付 + 通知 |
| **M2（可灰度）** | P1 全部 | 补偿机制齐全，敢放真实流量 |
| **M3（可多租户售卖）** | P2 全部 | 隔离 + 密钥安全达标 |
| **M4（打磨）** | P3 | 对账、体验、可观测性完善 |

---

## 四、V1 业务闭环参考流程

```text
商户 API 安全 → 订单 → PSP 适配 → 账务 → Outbox/通知 → 定时补偿 → 后台查询
```

代收：
```text
商户创建 pay_order → 选 PSP → 请求 PSP 下单 → 保存返回 → 接收回调
→ 校验/幂等 → 代收成功入账 → 建通知任务 → 异步通知商户
```

代付：
```text
商户创建 payout_order → 校验余额 → 冻结余额 → 请求 PSP 代付 → 接收回调/定时补查
→ 成功扣冻结 / 失败解冻 → 建通知任务 → 异步通知商户
```
