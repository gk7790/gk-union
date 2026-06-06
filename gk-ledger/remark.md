# 总账系统

```aiignore
gk-ledger
├── Account（账户）
├── Ledger（分录）
├── Settlement（结算）
├── Statement（账单）
├── Reconciliation（对账）
├── Event（事件）
```

# 核心表（第一阶段必须有）

1. edger_subject（资金主体）
   资金属于谁。
```aiignore
CREATE TABLE ledger_subject (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    subject_no VARCHAR(32) NOT NULL,
    subject_type VARCHAR(32) NOT NULL,
    -- MERCHANT
    -- PSP
    -- PLATFORM
    -- AGENT

    subject_name VARCHAR(128),

    status TINYINT DEFAULT 1,

    created_time DATETIME NOT NULL,
    updated_time DATETIME NOT NULL,

    UNIQUE KEY uk_subject_no(subject_no)
);
```
2. 
2. 