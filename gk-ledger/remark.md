# 总账系统

# 核心表（第一阶段必须有）

### 核心账户模型
```aiignore
ledger_account      账户定义
ledger_balance      当前余额
ledger_journal      记账凭证
ledger_entry        账变流水 / 会计分录
ledger_hold         冻结明细
```
- ledger_account 表示一个账户，不是银行卡，而是系统内部会计账户(每个租户、商户、PSP、币种都应独立开户)。
- ledger_journal：账务凭证头(这样可以防止 PSP 回调、商户重试、定时任务重复入账。)
- ledger_balance：账户余额缓存(更新余额时对 account_id + currency 加行锁，所有账户按 account_id 升序锁定，减少死锁。)
- ledger_journal：凭证头 带业务幂等键，一次业务事件只生成一张。
- ledger_entry：账变流水，账变流水/会计分录，已包含 balance_change、balance_before、balance_after。 也是复式分录。
- ledger_hold: 冻结明细，记录冻结、释放、消耗、剩余金额。


商户余额、PSP 余额、多币种、冻结释放、代收、代付、结算、冲正、对账调账，都可以统一走 ledger_journal + ledger_entry，不会把金额修改散落到业务表里。