# 国际四方 V1 (第一版) 
目标是：**能稳定完成一笔代收、一笔代付，钱账正确，可追溯，可补偿**。先把主链路打穿。

**V1 必做功能**

1. 租户与商户基础

```text
sys_tenant
merchant
merchant_app
```

支持租户隔离、商户管理、商户应用 `app_id / api_secret`、IP 白名单、回调地址。

2. PSP 基础配置

```text
psp_provider
psp_method
psp_route_rule
```

先支持 1 到 2 个 PSP 即可。路由规则可以简单点：按国家、币种、支付方式、代收/代付方向匹配。

3. 商户开放 API

至少做：

```text
创建代收订单
查询代收订单
创建代付订单
查询代付订单
商户余额查询
```

必须有签名、时间戳、nonce 防重放、幂等请求号。

4. 代收订单主链路

```text
商户创建 pay_order
平台选 PSP
请求 PSP 下单
保存 PSP 返回参数
接收 PSP 回调
代收成功后入账
写 mq_outbox 事件
商户异步通知
```

5. 代付订单主链路

```text
商户创建 payout_order
校验余额
冻结商户余额
请求 PSP 代付
接收 PSP 回调 / 定时查询状态
成功扣冻结余额
失败解冻
写 mq_outbox 事件
商户异步通知
```

6. 账务核心

V1 必须有：

```text
ledger_account
ledger_balance
ledger_journal
ledger_entry
ledger_hold
```

支持：

```text
商户开户
PSP 开户
代收成功入账
待结算释放到可用
代付冻结
代付成功
代付失败解冻
账务流水查询
商户余额查询
```

7. Outbox 事件机制

暂时不用 RocketMQ，但要做：

```text
mq_outbox
mq_consume_record
Quartz 扫描
失败重试
死信状态
```

主要用于：

```text
商户通知
PSP 状态补查询
结算释放
```

8. 商户通知

```text
notify_task
notify_record
```

或者先复用 `mq_outbox` 也可以，但我建议通知单独建表，因为通知有 URL、HTTP 状态码、响应体、签名、重试次数。

必须支持：

```text
成功通知
失败重试
手动重发
通知日志查询
```

9. 定时任务

用现有 `gk-scheduler` 做：

```text
代收超时关闭
代付处理中补查询
商户通知重试
Outbox 事件重试
待结算释放
```

10. 后台管理

V1 后台最少要有：

```text
商户管理
商户应用管理
PSP 管理
支付方式管理
路由规则管理
代收订单查询
代付订单查询
账务流水查询
商户余额查询
通知记录查询
```

**V1 可以先不做**

这些先别急：

```text
复杂清分分润
代理商多级分润
多 PSP 智能路由
实时风控引擎
自动对账差错处理
多语言商户门户
RocketMQ
Kafka
复杂报表数据仓库
商户自助后台
```

**V1版本业务流程**

```text
商户 API 安全
  ↓
订单
  ↓
PSP 适配
  ↓
账务
  ↓
Outbox
  ↓
通知
  ↓
定时补偿
  ↓
后台查询
```

一句话：  
**V1 的验收标准不是功能多，而是每一笔钱从订单、PSP、账务、余额、通知都能闭环追踪。**