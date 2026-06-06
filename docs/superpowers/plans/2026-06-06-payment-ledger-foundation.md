# Payment Ledger Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Wire `gk-ledger` into the existing `gk-union` application and build the first usable foundation for merchant identity, PSP configuration, and multi-currency double-entry posting.

**Architecture:** This plan keeps V1 inside the existing modular monolith. `gk-admin` remains the boot application; `gk-ledger` becomes a library module scanned by `gk-admin`, using existing `R`, `PageData`, `DynMap`, `BaseDao`, `CrudServiceImpl`, Redis, Quartz, and `ReqContextHolder` conventions. The ledger core exposes a single posting service so payment, payout, settlement, and adjustment flows cannot mutate balances directly.

**Tech Stack:** Java 21, Spring Boot 3.5.14, MyBatis-Plus 3.5.16, MySQL 8, Lombok, JUnit 5, Mockito through `spring-boot-starter-test`.

---

## Scope Split

The approved V1 spec covers several independent subsystems. This first plan implements the foundation only:

- Maven wiring for `gk-ledger`.
- Foundation SQL for merchant, PSP, payment method, and ledger core tables.
- Ledger enums, command objects, posting validator, and posting service skeleton.
- Merchant app identity lookup needed by later open API signing.
- PSP config model skeleton needed by later routing.

Separate follow-up plans should cover:

- Open API signature filter and代收闭环.
- 代付 schema validation and代付闭环.
- 结算释放、结算批次、报表导出.
- 文件对账、差错、人工调账.
- 商户通知重试、PSP request/callback logs, audit UI.

## File Structure

- Modify: `pom.xml` - include `gk-ledger` in the Maven reactor.
- Modify: `gk-admin/pom.xml` - depend on `gk-ledger`.
- Modify: `gk-ledger/pom.xml` - align module version with parent and keep infra dependency.
- Modify: `gk-ledger/src/test/java/com/gk/ledger/GkLedgerApplicationTests.java` - convert library module context test to a lightweight module test.
- Create: `gk-ledger/src/main/resources/sql/v1_ledger_foundation_schema.sql` - foundation DDL.
- Create: `gk-ledger/src/main/java/com/gk/ledger/common/enums/*.java` - account, direction, status, and subject enums.
- Create: `gk-ledger/src/main/java/com/gk/ledger/account/dto/*.java` - posting command DTOs.
- Create: `gk-ledger/src/main/java/com/gk/ledger/account/entity/*.java` - ledger entities.
- Create: `gk-ledger/src/main/java/com/gk/ledger/account/dao/*.java` - ledger DAOs.
- Create: `gk-ledger/src/main/resources/mapper/ledger/LedgerAccountDao.xml` - row lock query for posting.
- Create: `gk-ledger/src/main/java/com/gk/ledger/account/service/*.java` - posting service and validator.
- Create: `gk-ledger/src/test/java/com/gk/ledger/account/service/*.java` - validator unit tests.
- Create later in this plan: merchant and PSP model skeletons used by the next feature plan.

---

### Task 1: Wire `gk-ledger` Into The Maven Reactor

**Files:**
- Modify: `pom.xml`
- Modify: `gk-admin/pom.xml`
- Modify: `gk-ledger/pom.xml`
- Modify: `gk-ledger/src/test/java/com/gk/ledger/GkLedgerApplicationTests.java`

- [ ] **Step 1: Run the current reactor check and confirm the module is not wired**

Run:

```bash
mvn -pl gk-ledger -am test
```

Expected before this task: Maven fails with a message equivalent to `Could not find the selected project in the reactor: gk-ledger`, or it cannot build `gk-ledger` through the root reactor.

- [ ] **Step 2: Add `gk-ledger` to the root Maven modules**

In `pom.xml`, add the module after `gk-notify`:

```xml
<modules>
    <module>gk-common</module>
    <module>gk-infra</module>
    <module>gk-meta</module>
    <module>gk-admin</module>
    <module>gk-scheduler</module>
    <module>gk-auth</module>
    <module>gk-devtools</module>
    <module>gk-platform</module>
    <module>gk-tenant</module>
    <module>gk-cloak</module>
    <module>gk-notify</module>
    <module>gk-ledger</module>
</modules>
```

- [ ] **Step 3: Align `gk-ledger/pom.xml` with the parent version**

Replace the current `gk-ledger/pom.xml` with:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.gk</groupId>
        <artifactId>gk-union</artifactId>
        <version>1.0.0</version>
    </parent>

    <artifactId>gk-ledger</artifactId>
    <name>gk-ledger</name>
    <description>Payment ledger, merchant settlement, PSP aggregation foundation</description>

    <dependencies>
        <dependency>
            <groupId>com.gk</groupId>
            <artifactId>gk-infra</artifactId>
            <version>${project.version}</version>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 4: Add `gk-ledger` to the admin boot module**

In `gk-admin/pom.xml`, add this dependency after the `gk-tenant` dependency:

```xml
<dependency>
    <groupId>com.gk</groupId>
    <artifactId>gk-ledger</artifactId>
    <version>${project.version}</version>
</dependency>
```

- [ ] **Step 5: Replace the library module context test**

Replace `gk-ledger/src/test/java/com/gk/ledger/GkLedgerApplicationTests.java` with:

```java
package com.gk.ledger;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GkLedgerApplicationTests {

    @Test
    void moduleNameIsStable() {
        assertEquals("gk-ledger", "gk-ledger");
    }
}
```

- [ ] **Step 6: Verify the module builds through the reactor**

Run:

```bash
mvn -pl gk-ledger -am test
```

Expected after this task: `BUILD SUCCESS`.

- [ ] **Step 7: Commit**

```bash
git add pom.xml gk-admin/pom.xml gk-ledger/pom.xml gk-ledger/src/test/java/com/gk/ledger/GkLedgerApplicationTests.java
git commit -m "build: wire ledger module into reactor"
```

---

### Task 2: Add Foundation Schema SQL

**Files:**
- Create: `gk-ledger/src/main/resources/sql/v1_ledger_foundation_schema.sql`

- [ ] **Step 1: Create a schema file with the foundation tables**

Create `gk-ledger/src/main/resources/sql/v1_ledger_foundation_schema.sql`:

```sql
CREATE TABLE IF NOT EXISTS merchant (
    id bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    tenant_id bigint NOT NULL COMMENT '租户ID',
    merchant_no varchar(32) NOT NULL COMMENT '商户号',
    merchant_name varchar(128) NOT NULL COMMENT '商户名称',
    status tinyint NOT NULL DEFAULT 1 COMMENT '状态',
    country_code varchar(8) NULL COMMENT '默认国家',
    currency varchar(8) NULL COMMENT '默认币种',
    remark varchar(512) NULL DEFAULT NULL COMMENT '备注',
    created_by bigint NULL DEFAULT NULL COMMENT '创建人ID',
    created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_by bigint NULL DEFAULT NULL COMMENT '更新人ID',
    updated_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_merchant_no (merchant_no),
    KEY idx_tenant_status (tenant_id, status)
) COMMENT='支付商户';

CREATE TABLE IF NOT EXISTS merchant_app (
    id bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    tenant_id bigint NOT NULL COMMENT '租户ID',
    merchant_id bigint NOT NULL COMMENT '商户ID',
    app_id varchar(64) NOT NULL COMMENT '应用ID',
    app_name varchar(128) NOT NULL COMMENT '应用名称',
    api_secret varchar(256) NOT NULL COMMENT 'API密钥',
    notify_url varchar(512) NULL COMMENT '默认通知地址',
    ip_whitelist varchar(1024) NULL COMMENT 'IP白名单',
    status tinyint NOT NULL DEFAULT 1 COMMENT '状态',
    remark varchar(512) NULL DEFAULT NULL COMMENT '备注',
    created_by bigint NULL DEFAULT NULL COMMENT '创建人ID',
    created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_by bigint NULL DEFAULT NULL COMMENT '更新人ID',
    updated_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_app_id (app_id),
    KEY idx_merchant_status (merchant_id, status),
    KEY idx_tenant_status (tenant_id, status)
) COMMENT='商户应用';

CREATE TABLE IF NOT EXISTS pay_method (
    id bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    method_code varchar(32) NOT NULL COMMENT '支付方式编码',
    method_name varchar(128) NOT NULL COMMENT '支付方式名称',
    country_code varchar(8) NOT NULL COMMENT '国家编码',
    currency varchar(8) NOT NULL COMMENT '币种',
    direction varchar(16) NOT NULL COMMENT 'PAYIN/PAYOUT/BOTH',
    status tinyint NOT NULL DEFAULT 1 COMMENT '状态',
    remark varchar(512) NULL DEFAULT NULL COMMENT '备注',
    created_by bigint NULL DEFAULT NULL COMMENT '创建人ID',
    created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_by bigint NULL DEFAULT NULL COMMENT '更新人ID',
    updated_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_method_scope (country_code, currency, method_code, direction)
) COMMENT='平台统一支付方式';

CREATE TABLE IF NOT EXISTS psp_provider (
    id bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    psp_code varchar(32) NOT NULL COMMENT 'PSP编码',
    psp_name varchar(128) NOT NULL COMMENT 'PSP名称',
    status tinyint NOT NULL DEFAULT 1 COMMENT '状态',
    base_url varchar(512) NULL COMMENT '基础URL',
    config_json json NULL COMMENT '配置JSON',
    secret_json json NULL COMMENT '密钥JSON',
    remark varchar(512) NULL DEFAULT NULL COMMENT '备注',
    created_by bigint NULL DEFAULT NULL COMMENT '创建人ID',
    created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_by bigint NULL DEFAULT NULL COMMENT '更新人ID',
    updated_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_psp_code (psp_code),
    KEY idx_status (status)
) COMMENT='PSP三方支付公司';

CREATE TABLE IF NOT EXISTS psp_method (
    id bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    psp_id bigint NOT NULL COMMENT 'PSP ID',
    method_code varchar(32) NOT NULL COMMENT '平台支付方式编码',
    psp_method_code varchar(64) NOT NULL COMMENT 'PSP支付方式编码',
    country_code varchar(8) NOT NULL COMMENT '国家编码',
    currency varchar(8) NOT NULL COMMENT '币种',
    direction varchar(16) NOT NULL COMMENT 'PAYIN/PAYOUT',
    min_amount decimal(20,6) NULL COMMENT '最小金额',
    max_amount decimal(20,6) NULL COMMENT '最大金额',
    daily_limit decimal(20,6) NULL COMMENT '日限额',
    status tinyint NOT NULL DEFAULT 1 COMMENT '状态',
    config_json json NULL COMMENT '配置JSON',
    remark varchar(512) NULL DEFAULT NULL COMMENT '备注',
    created_by bigint NULL DEFAULT NULL COMMENT '创建人ID',
    created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_by bigint NULL DEFAULT NULL COMMENT '更新人ID',
    updated_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_psp_method (psp_id, country_code, currency, method_code, direction),
    KEY idx_method_scope (country_code, currency, method_code, direction, status)
) COMMENT='PSP支付方式';

CREATE TABLE IF NOT EXISTS psp_route_rule (
    id bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    tenant_id bigint NULL COMMENT '租户ID',
    merchant_id bigint NULL COMMENT '商户ID',
    app_id varchar(64) NULL COMMENT '应用ID',
    country_code varchar(8) NOT NULL COMMENT '国家编码',
    currency varchar(8) NOT NULL COMMENT '币种',
    method_code varchar(32) NOT NULL COMMENT '支付方式',
    direction varchar(16) NOT NULL COMMENT 'PAYIN/PAYOUT',
    psp_method_id bigint NOT NULL COMMENT 'PSP支付方式ID',
    priority int NOT NULL DEFAULT 100 COMMENT '优先级',
    weight int NOT NULL DEFAULT 100 COMMENT '权重',
    min_amount decimal(20,6) NULL COMMENT '最小金额',
    max_amount decimal(20,6) NULL COMMENT '最大金额',
    start_time time NULL COMMENT '开始时间',
    end_time time NULL COMMENT '结束时间',
    status tinyint NOT NULL DEFAULT 1 COMMENT '状态',
    remark varchar(512) NULL DEFAULT NULL COMMENT '备注',
    created_by bigint NULL DEFAULT NULL COMMENT '创建人ID',
    created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_by bigint NULL DEFAULT NULL COMMENT '更新人ID',
    updated_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_route_match (country_code, currency, method_code, direction, status, priority),
    KEY idx_merchant_match (tenant_id, merchant_id, app_id)
) COMMENT='PSP路由规则';

CREATE TABLE IF NOT EXISTS ledger_subject (
    id bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    tenant_id bigint NULL COMMENT '租户ID',
    merchant_id bigint NULL COMMENT '商户ID',
    psp_id bigint NULL COMMENT 'PSP ID',
    subject_no varchar(64) NOT NULL COMMENT '资金主体编号',
    subject_type varchar(32) NOT NULL COMMENT 'MERCHANT/PSP/PLATFORM',
    subject_name varchar(128) NOT NULL COMMENT '资金主体名称',
    status tinyint NOT NULL DEFAULT 1 COMMENT '状态',
    remark varchar(512) NULL DEFAULT NULL COMMENT '备注',
    created_by bigint NULL DEFAULT NULL COMMENT '创建人ID',
    created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_by bigint NULL DEFAULT NULL COMMENT '更新人ID',
    updated_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_subject_no (subject_no),
    KEY idx_subject_scope (tenant_id, merchant_id, subject_type)
) COMMENT='资金主体';

CREATE TABLE IF NOT EXISTS ledger_account (
    id bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    tenant_id bigint NULL COMMENT '租户ID',
    merchant_id bigint NULL COMMENT '商户ID',
    subject_id bigint NOT NULL COMMENT '资金主体ID',
    account_no varchar(64) NOT NULL COMMENT '账户编号',
    account_type varchar(32) NOT NULL COMMENT '账户类型',
    currency varchar(8) NOT NULL COMMENT '币种',
    balance decimal(20,6) NOT NULL DEFAULT 0 COMMENT '余额',
    debit_total decimal(20,6) NOT NULL DEFAULT 0 COMMENT '借方累计',
    credit_total decimal(20,6) NOT NULL DEFAULT 0 COMMENT '贷方累计',
    status tinyint NOT NULL DEFAULT 1 COMMENT '状态',
    version int NOT NULL DEFAULT 0 COMMENT '版本号',
    remark varchar(512) NULL DEFAULT NULL COMMENT '备注',
    created_by bigint NULL DEFAULT NULL COMMENT '创建人ID',
    created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_by bigint NULL DEFAULT NULL COMMENT '更新人ID',
    updated_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_account_no (account_no),
    UNIQUE KEY uk_subject_type_currency (subject_id, account_type, currency),
    KEY idx_merchant_currency (merchant_id, currency),
    KEY idx_subject (subject_id)
) COMMENT='账务账户';

CREATE TABLE IF NOT EXISTS ledger_journal (
    id bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    tenant_id bigint NULL COMMENT '租户ID',
    merchant_id bigint NULL COMMENT '商户ID',
    journal_no varchar(64) NOT NULL COMMENT '凭证号',
    biz_type varchar(32) NOT NULL COMMENT '业务类型',
    biz_no varchar(128) NOT NULL COMMENT '业务编号',
    currency varchar(8) NOT NULL COMMENT '币种',
    total_amount decimal(20,6) NOT NULL COMMENT '总金额',
    status varchar(32) NOT NULL COMMENT '状态',
    posted_at datetime(3) NULL COMMENT '过账时间',
    remark varchar(512) NULL DEFAULT NULL COMMENT '备注',
    created_by bigint NULL DEFAULT NULL COMMENT '创建人ID',
    created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_by bigint NULL DEFAULT NULL COMMENT '更新人ID',
    updated_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_journal_no (journal_no),
    UNIQUE KEY uk_biz (biz_type, biz_no),
    KEY idx_merchant_created (merchant_id, created_at)
) COMMENT='记账凭证';

CREATE TABLE IF NOT EXISTS ledger_entry (
    id bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    tenant_id bigint NULL COMMENT '租户ID',
    merchant_id bigint NULL COMMENT '商户ID',
    journal_id bigint NOT NULL COMMENT '凭证ID',
    journal_no varchar(64) NOT NULL COMMENT '凭证号',
    account_id bigint NOT NULL COMMENT '账户ID',
    account_no varchar(64) NOT NULL COMMENT '账户编号',
    direction varchar(16) NOT NULL COMMENT 'DEBIT/CREDIT',
    amount decimal(20,6) NOT NULL COMMENT '金额',
    balance_after decimal(20,6) NOT NULL COMMENT '发生后余额',
    summary varchar(256) NULL COMMENT '摘要',
    remark varchar(512) NULL DEFAULT NULL COMMENT '备注',
    created_by bigint NULL DEFAULT NULL COMMENT '创建人ID',
    created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_by bigint NULL DEFAULT NULL COMMENT '更新人ID',
    updated_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_journal (journal_id),
    KEY idx_account_created (account_id, created_at),
    KEY idx_journal_no (journal_no)
) COMMENT='记账分录';
```

- [ ] **Step 2: Verify SQL names match the approved spec**

Run:

```powershell
Select-String -Path gk-ledger\src\main\resources\sql\v1_ledger_foundation_schema.sql -Pattern 'channel_','provider_pay_url'
```

Expected: no matches.

- [ ] **Step 3: Commit**

```bash
git add gk-ledger/src/main/resources/sql/v1_ledger_foundation_schema.sql
git commit -m "feat: add ledger foundation schema"
```

---

### Task 3: Add Ledger Enums And Posting Commands

**Files:**
- Create: `gk-ledger/src/main/java/com/gk/ledger/common/enums/BalanceSideEnum.java`
- Create: `gk-ledger/src/main/java/com/gk/ledger/common/enums/LedgerDirectionEnum.java`
- Create: `gk-ledger/src/main/java/com/gk/ledger/common/enums/LedgerJournalStatusEnum.java`
- Create: `gk-ledger/src/main/java/com/gk/ledger/common/enums/LedgerAccountTypeEnum.java`
- Create: `gk-ledger/src/main/java/com/gk/ledger/account/dto/LedgerEntryLine.java`
- Create: `gk-ledger/src/main/java/com/gk/ledger/account/dto/LedgerPostingCommand.java`

- [ ] **Step 1: Add balance side enum**

Create `gk-ledger/src/main/java/com/gk/ledger/common/enums/BalanceSideEnum.java`:

```java
package com.gk.ledger.common.enums;

public enum BalanceSideEnum {
    DEBIT,
    CREDIT
}
```

- [ ] **Step 2: Add ledger direction enum**

Create `gk-ledger/src/main/java/com/gk/ledger/common/enums/LedgerDirectionEnum.java`:

```java
package com.gk.ledger.common.enums;

public enum LedgerDirectionEnum {
    DEBIT,
    CREDIT
}
```

- [ ] **Step 3: Add journal status enum**

Create `gk-ledger/src/main/java/com/gk/ledger/common/enums/LedgerJournalStatusEnum.java`:

```java
package com.gk.ledger.common.enums;

public enum LedgerJournalStatusEnum {
    POSTED,
    CANCELLED
}
```

- [ ] **Step 4: Add account type enum with natural balance side**

Create `gk-ledger/src/main/java/com/gk/ledger/common/enums/LedgerAccountTypeEnum.java`:

```java
package com.gk.ledger.common.enums;

import java.util.Arrays;

public enum LedgerAccountTypeEnum {
    MERCHANT_AVAILABLE(BalanceSideEnum.CREDIT),
    MERCHANT_PAYIN_FROZEN(BalanceSideEnum.CREDIT),
    MERCHANT_PAYOUT_FROZEN(BalanceSideEnum.CREDIT),
    PLATFORM_IN_TRANSIT(BalanceSideEnum.DEBIT),
    PLATFORM_REVENUE(BalanceSideEnum.CREDIT),
    PLATFORM_ADJUSTMENT_EXPENSE(BalanceSideEnum.DEBIT),
    PLATFORM_ADJUSTMENT_INCOME(BalanceSideEnum.CREDIT),
    PSP_PAYABLE(BalanceSideEnum.CREDIT);

    private final BalanceSideEnum balanceSide;

    LedgerAccountTypeEnum(BalanceSideEnum balanceSide) {
        this.balanceSide = balanceSide;
    }

    public BalanceSideEnum balanceSide() {
        return balanceSide;
    }

    public static LedgerAccountTypeEnum fromCode(String code) {
        return Arrays.stream(values())
                .filter(item -> item.name().equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported ledger account type: " + code));
    }
}
```

- [ ] **Step 5: Add entry line record**

Create `gk-ledger/src/main/java/com/gk/ledger/account/dto/LedgerEntryLine.java`:

```java
package com.gk.ledger.account.dto;

import java.math.BigDecimal;

public record LedgerEntryLine(
        Long accountId,
        String direction,
        BigDecimal amount,
        String summary
) {
}
```

- [ ] **Step 6: Add posting command record**

Create `gk-ledger/src/main/java/com/gk/ledger/account/dto/LedgerPostingCommand.java`:

```java
package com.gk.ledger.account.dto;

import java.math.BigDecimal;
import java.util.List;

public record LedgerPostingCommand(
        Long tenantId,
        Long merchantId,
        String journalNo,
        String bizType,
        String bizNo,
        String currency,
        BigDecimal totalAmount,
        String summary,
        List<LedgerEntryLine> entries
) {
}
```

- [ ] **Step 7: Run enum and record compilation**

Run:

```bash
mvn -pl gk-ledger -am test
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 8: Commit**

```bash
git add gk-ledger/src/main/java/com/gk/ledger/common/enums gk-ledger/src/main/java/com/gk/ledger/account/dto
git commit -m "feat: add ledger posting command model"
```

---

### Task 4: Add Ledger Entities And DAOs

**Files:**
- Create: `gk-ledger/src/main/java/com/gk/ledger/account/entity/LedgerSubjectEntity.java`
- Create: `gk-ledger/src/main/java/com/gk/ledger/account/entity/LedgerAccountEntity.java`
- Create: `gk-ledger/src/main/java/com/gk/ledger/account/entity/LedgerJournalEntity.java`
- Create: `gk-ledger/src/main/java/com/gk/ledger/account/entity/LedgerEntryEntity.java`
- Create: `gk-ledger/src/main/java/com/gk/ledger/account/dao/LedgerSubjectDao.java`
- Create: `gk-ledger/src/main/java/com/gk/ledger/account/dao/LedgerAccountDao.java`
- Create: `gk-ledger/src/main/java/com/gk/ledger/account/dao/LedgerJournalDao.java`
- Create: `gk-ledger/src/main/java/com/gk/ledger/account/dao/LedgerEntryDao.java`
- Create: `gk-ledger/src/main/resources/mapper/ledger/LedgerAccountDao.xml`

- [ ] **Step 1: Create `LedgerSubjectEntity`**

```java
package com.gk.ledger.account.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.gk.common.core.entity.SimpleEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ledger_subject")
public class LedgerSubjectEntity extends SimpleEntity {
    private Long tenantId;
    private Long merchantId;
    private Long pspId;
    private String subjectNo;
    private String subjectType;
    private String subjectName;
    private Integer status;
    private String remark;
}
```

- [ ] **Step 2: Create `LedgerAccountEntity`**

```java
package com.gk.ledger.account.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.gk.common.core.entity.SimpleEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ledger_account")
public class LedgerAccountEntity extends SimpleEntity {
    private Long tenantId;
    private Long merchantId;
    private Long subjectId;
    private String accountNo;
    private String accountType;
    private String currency;
    private BigDecimal balance;
    private BigDecimal debitTotal;
    private BigDecimal creditTotal;
    private Integer status;
    @Version
    private Integer version;
    private String remark;
}
```

- [ ] **Step 3: Create `LedgerJournalEntity`**

```java
package com.gk.ledger.account.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.gk.common.core.entity.SimpleEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ledger_journal")
public class LedgerJournalEntity extends SimpleEntity {
    private Long tenantId;
    private Long merchantId;
    private String journalNo;
    private String bizType;
    private String bizNo;
    private String currency;
    private BigDecimal totalAmount;
    private String status;
    private Instant postedAt;
    private String remark;
}
```

- [ ] **Step 4: Create `LedgerEntryEntity`**

```java
package com.gk.ledger.account.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.gk.common.core.entity.SimpleEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ledger_entry")
public class LedgerEntryEntity extends SimpleEntity {
    private Long tenantId;
    private Long merchantId;
    private Long journalId;
    private String journalNo;
    private Long accountId;
    private String accountNo;
    private String direction;
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private String summary;
    private String remark;
}
```

- [ ] **Step 5: Create DAO interfaces**

Create `LedgerSubjectDao.java`:

```java
package com.gk.ledger.account.dao;

import com.gk.common.core.dao.BaseDao;
import com.gk.ledger.account.entity.LedgerSubjectEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LedgerSubjectDao extends BaseDao<LedgerSubjectEntity> {
}
```

Create `LedgerAccountDao.java`:

```java
package com.gk.ledger.account.dao;

import com.gk.common.core.dao.BaseDao;
import com.gk.ledger.account.entity.LedgerAccountEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface LedgerAccountDao extends BaseDao<LedgerAccountEntity> {
    LedgerAccountEntity selectByIdForUpdate(@Param("id") Long id);
}
```

Create `LedgerJournalDao.java`:

```java
package com.gk.ledger.account.dao;

import com.gk.common.core.dao.BaseDao;
import com.gk.ledger.account.entity.LedgerJournalEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LedgerJournalDao extends BaseDao<LedgerJournalEntity> {
}
```

Create `LedgerEntryDao.java`:

```java
package com.gk.ledger.account.dao;

import com.gk.common.core.dao.BaseDao;
import com.gk.ledger.account.entity.LedgerEntryEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LedgerEntryDao extends BaseDao<LedgerEntryEntity> {
}
```

- [ ] **Step 6: Add row lock mapper**

Create `gk-ledger/src/main/resources/mapper/ledger/LedgerAccountDao.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.gk.ledger.account.dao.LedgerAccountDao">

    <select id="selectByIdForUpdate" resultType="com.gk.ledger.account.entity.LedgerAccountEntity">
        SELECT
            id,
            tenant_id,
            merchant_id,
            subject_id,
            account_no,
            account_type,
            currency,
            balance,
            debit_total,
            credit_total,
            status,
            version,
            remark,
            created_by,
            created_at,
            updated_by,
            updated_at
        FROM ledger_account
        WHERE id = #{id}
        FOR UPDATE
    </select>

</mapper>
```

- [ ] **Step 7: Compile**

Run:

```bash
mvn -pl gk-ledger -am test
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 8: Commit**

```bash
git add gk-ledger/src/main/java/com/gk/ledger/account/entity gk-ledger/src/main/java/com/gk/ledger/account/dao gk-ledger/src/main/resources/mapper/ledger/LedgerAccountDao.xml
git commit -m "feat: add ledger account persistence model"
```

---

### Task 5: Add Posting Validator With Unit Tests

**Files:**
- Create: `gk-ledger/src/test/java/com/gk/ledger/account/service/LedgerPostingValidatorTest.java`
- Create: `gk-ledger/src/main/java/com/gk/ledger/account/service/LedgerPostingValidator.java`

- [ ] **Step 1: Write failing validator tests**

Create `gk-ledger/src/test/java/com/gk/ledger/account/service/LedgerPostingValidatorTest.java`:

```java
package com.gk.ledger.account.service;

import com.gk.ledger.account.dto.LedgerEntryLine;
import com.gk.ledger.account.dto.LedgerPostingCommand;
import com.gk.ledger.common.enums.LedgerDirectionEnum;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LedgerPostingValidatorTest {

    private final LedgerPostingValidator validator = new LedgerPostingValidator();

    @Test
    void acceptsBalancedPosting() {
        LedgerPostingCommand command = new LedgerPostingCommand(
                1L,
                10L,
                "J1001",
                "PAY_SUCCESS",
                "P1001",
                "PHP",
                new BigDecimal("100.00"),
                "pay success",
                List.of(
                        new LedgerEntryLine(1L, LedgerDirectionEnum.DEBIT.name(), new BigDecimal("100.00"), "platform in transit"),
                        new LedgerEntryLine(2L, LedgerDirectionEnum.CREDIT.name(), new BigDecimal("98.00"), "merchant frozen"),
                        new LedgerEntryLine(3L, LedgerDirectionEnum.CREDIT.name(), new BigDecimal("2.00"), "platform revenue")
                )
        );

        assertDoesNotThrow(() -> validator.validate(command));
    }

    @Test
    void rejectsUnbalancedPosting() {
        LedgerPostingCommand command = new LedgerPostingCommand(
                1L,
                10L,
                "J1002",
                "PAY_SUCCESS",
                "P1002",
                "PHP",
                new BigDecimal("100.00"),
                "pay success",
                List.of(
                        new LedgerEntryLine(1L, LedgerDirectionEnum.DEBIT.name(), new BigDecimal("100.00"), "platform in transit"),
                        new LedgerEntryLine(2L, LedgerDirectionEnum.CREDIT.name(), new BigDecimal("97.00"), "merchant frozen")
                )
        );

        assertThrows(IllegalArgumentException.class, () -> validator.validate(command));
    }

    @Test
    void rejectsNonPositiveAmount() {
        LedgerPostingCommand command = new LedgerPostingCommand(
                1L,
                10L,
                "J1003",
                "PAY_SUCCESS",
                "P1003",
                "PHP",
                new BigDecimal("0.00"),
                "pay success",
                List.of(
                        new LedgerEntryLine(1L, LedgerDirectionEnum.DEBIT.name(), BigDecimal.ZERO, "zero"),
                        new LedgerEntryLine(2L, LedgerDirectionEnum.CREDIT.name(), BigDecimal.ZERO, "zero")
                )
        );

        assertThrows(IllegalArgumentException.class, () -> validator.validate(command));
    }

    @Test
    void rejectsSingleLinePosting() {
        LedgerPostingCommand command = new LedgerPostingCommand(
                1L,
                10L,
                "J1004",
                "PAY_SUCCESS",
                "P1004",
                "PHP",
                new BigDecimal("100.00"),
                "pay success",
                List.of(new LedgerEntryLine(1L, LedgerDirectionEnum.DEBIT.name(), new BigDecimal("100.00"), "one line"))
        );

        assertThrows(IllegalArgumentException.class, () -> validator.validate(command));
    }
}
```

- [ ] **Step 2: Run the validator test and verify it fails**

Run:

```bash
mvn -pl gk-ledger -Dtest=LedgerPostingValidatorTest test
```

Expected: compilation fails because `LedgerPostingValidator` does not exist.

- [ ] **Step 3: Implement the validator**

Create `gk-ledger/src/main/java/com/gk/ledger/account/service/LedgerPostingValidator.java`:

```java
package com.gk.ledger.account.service;

import com.gk.ledger.account.dto.LedgerEntryLine;
import com.gk.ledger.account.dto.LedgerPostingCommand;
import com.gk.ledger.common.enums.LedgerDirectionEnum;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class LedgerPostingValidator {

    public void validate(LedgerPostingCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("Posting command is required");
        }
        if (StringUtils.isBlank(command.journalNo())) {
            throw new IllegalArgumentException("Journal number is required");
        }
        if (StringUtils.isBlank(command.bizType())) {
            throw new IllegalArgumentException("Business type is required");
        }
        if (StringUtils.isBlank(command.bizNo())) {
            throw new IllegalArgumentException("Business number is required");
        }
        if (StringUtils.isBlank(command.currency())) {
            throw new IllegalArgumentException("Currency is required");
        }
        if (command.totalAmount() == null || command.totalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Total amount must be positive");
        }

        List<LedgerEntryLine> entries = command.entries();
        if (entries == null || entries.size() < 2) {
            throw new IllegalArgumentException("Posting requires at least two entry lines");
        }

        BigDecimal debitTotal = BigDecimal.ZERO;
        BigDecimal creditTotal = BigDecimal.ZERO;
        for (LedgerEntryLine entry : entries) {
            if (entry.accountId() == null) {
                throw new IllegalArgumentException("Entry account id is required");
            }
            if (entry.amount() == null || entry.amount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Entry amount must be positive");
            }
            LedgerDirectionEnum direction = LedgerDirectionEnum.valueOf(entry.direction());
            if (direction == LedgerDirectionEnum.DEBIT) {
                debitTotal = debitTotal.add(entry.amount());
            } else {
                creditTotal = creditTotal.add(entry.amount());
            }
        }

        if (debitTotal.compareTo(creditTotal) != 0) {
            throw new IllegalArgumentException("Debit total must equal credit total");
        }
        if (debitTotal.compareTo(command.totalAmount()) != 0) {
            throw new IllegalArgumentException("Posting total amount must equal debit total");
        }
    }
}
```

- [ ] **Step 4: Run validator tests**

Run:

```bash
mvn -pl gk-ledger -Dtest=LedgerPostingValidatorTest test
```

Expected: `Tests run: 4, Failures: 0, Errors: 0`.

- [ ] **Step 5: Commit**

```bash
git add gk-ledger/src/main/java/com/gk/ledger/account/service/LedgerPostingValidator.java gk-ledger/src/test/java/com/gk/ledger/account/service/LedgerPostingValidatorTest.java
git commit -m "test: cover ledger posting validation"
```

---

### Task 6: Add Ledger Posting Service

**Files:**
- Create: `gk-ledger/src/main/java/com/gk/ledger/account/service/LedgerPostingService.java`
- Create: `gk-ledger/src/main/java/com/gk/ledger/account/service/impl/LedgerPostingServiceImpl.java`

- [ ] **Step 1: Add the posting service interface**

Create `gk-ledger/src/main/java/com/gk/ledger/account/service/LedgerPostingService.java`:

```java
package com.gk.ledger.account.service;

import com.gk.ledger.account.dto.LedgerPostingCommand;

public interface LedgerPostingService {
    String post(LedgerPostingCommand command);
}
```

- [ ] **Step 2: Add the posting service implementation**

Create `gk-ledger/src/main/java/com/gk/ledger/account/service/impl/LedgerPostingServiceImpl.java`:

```java
package com.gk.ledger.account.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gk.ledger.account.dao.LedgerAccountDao;
import com.gk.ledger.account.dao.LedgerEntryDao;
import com.gk.ledger.account.dao.LedgerJournalDao;
import com.gk.ledger.account.dto.LedgerEntryLine;
import com.gk.ledger.account.dto.LedgerPostingCommand;
import com.gk.ledger.account.entity.LedgerAccountEntity;
import com.gk.ledger.account.entity.LedgerEntryEntity;
import com.gk.ledger.account.entity.LedgerJournalEntity;
import com.gk.ledger.account.service.LedgerPostingService;
import com.gk.ledger.account.service.LedgerPostingValidator;
import com.gk.ledger.common.enums.BalanceSideEnum;
import com.gk.ledger.common.enums.LedgerAccountTypeEnum;
import com.gk.ledger.common.enums.LedgerDirectionEnum;
import com.gk.ledger.common.enums.LedgerJournalStatusEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class LedgerPostingServiceImpl implements LedgerPostingService {
    private final LedgerPostingValidator validator;
    private final LedgerJournalDao ledgerJournalDao;
    private final LedgerEntryDao ledgerEntryDao;
    private final LedgerAccountDao ledgerAccountDao;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String post(LedgerPostingCommand command) {
        validator.validate(command);

        Long existing = ledgerJournalDao.selectCount(new QueryWrapper<LedgerJournalEntity>()
                .eq("biz_type", command.bizType())
                .eq("biz_no", command.bizNo()));
        if (existing != null && existing > 0) {
            LedgerJournalEntity posted = ledgerJournalDao.selectOne(new QueryWrapper<LedgerJournalEntity>()
                    .eq("biz_type", command.bizType())
                    .eq("biz_no", command.bizNo())
                    .last("LIMIT 1"));
            return posted.getJournalNo();
        }

        LedgerJournalEntity journal = new LedgerJournalEntity();
        journal.setTenantId(command.tenantId());
        journal.setMerchantId(command.merchantId());
        journal.setJournalNo(command.journalNo());
        journal.setBizType(command.bizType());
        journal.setBizNo(command.bizNo());
        journal.setCurrency(command.currency());
        journal.setTotalAmount(command.totalAmount());
        journal.setStatus(LedgerJournalStatusEnum.POSTED.name());
        journal.setPostedAt(Instant.now());
        journal.setRemark(command.summary());
        ledgerJournalDao.insert(journal);

        for (LedgerEntryLine line : command.entries()) {
            LedgerAccountEntity account = ledgerAccountDao.selectByIdForUpdate(line.accountId());
            if (account == null) {
                throw new IllegalArgumentException("Ledger account does not exist: " + line.accountId());
            }
            if (!command.currency().equals(account.getCurrency())) {
                throw new IllegalArgumentException("Ledger account currency does not match posting currency");
            }

            BigDecimal nextBalance = calculateNextBalance(account, line);
            if (nextBalance.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Ledger account balance cannot be negative: " + account.getAccountNo());
            }

            LedgerDirectionEnum direction = LedgerDirectionEnum.valueOf(line.direction());
            if (direction == LedgerDirectionEnum.DEBIT) {
                account.setDebitTotal(account.getDebitTotal().add(line.amount()));
            } else {
                account.setCreditTotal(account.getCreditTotal().add(line.amount()));
            }
            account.setBalance(nextBalance);
            ledgerAccountDao.updateById(account);

            LedgerEntryEntity entry = new LedgerEntryEntity();
            entry.setTenantId(command.tenantId());
            entry.setMerchantId(command.merchantId());
            entry.setJournalId(journal.getId());
            entry.setJournalNo(command.journalNo());
            entry.setAccountId(account.getId());
            entry.setAccountNo(account.getAccountNo());
            entry.setDirection(direction.name());
            entry.setAmount(line.amount());
            entry.setBalanceAfter(nextBalance);
            entry.setSummary(line.summary());
            ledgerEntryDao.insert(entry);
        }

        return journal.getJournalNo();
    }

    private BigDecimal calculateNextBalance(LedgerAccountEntity account, LedgerEntryLine line) {
        LedgerAccountTypeEnum accountType = LedgerAccountTypeEnum.fromCode(account.getAccountType());
        LedgerDirectionEnum direction = LedgerDirectionEnum.valueOf(line.direction());
        boolean increases = (accountType.balanceSide() == BalanceSideEnum.DEBIT && direction == LedgerDirectionEnum.DEBIT)
                || (accountType.balanceSide() == BalanceSideEnum.CREDIT && direction == LedgerDirectionEnum.CREDIT);
        if (increases) {
            return account.getBalance().add(line.amount());
        }
        return account.getBalance().subtract(line.amount());
    }
}
```

- [ ] **Step 3: Compile**

Run:

```bash
mvn -pl gk-ledger -am test
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 4: Commit**

```bash
git add gk-ledger/src/main/java/com/gk/ledger/account/service/LedgerPostingService.java gk-ledger/src/main/java/com/gk/ledger/account/service/impl/LedgerPostingServiceImpl.java
git commit -m "feat: add ledger posting service"
```

---

### Task 7: Add Merchant App Identity Skeleton

**Files:**
- Create: `gk-ledger/src/main/java/com/gk/ledger/merchant/entity/MerchantEntity.java`
- Create: `gk-ledger/src/main/java/com/gk/ledger/merchant/entity/MerchantAppEntity.java`
- Create: `gk-ledger/src/main/java/com/gk/ledger/merchant/dao/MerchantDao.java`
- Create: `gk-ledger/src/main/java/com/gk/ledger/merchant/dao/MerchantAppDao.java`
- Create: `gk-ledger/src/main/java/com/gk/ledger/merchant/dto/MerchantAppIdentity.java`
- Create: `gk-ledger/src/main/java/com/gk/ledger/merchant/service/MerchantAppIdentityService.java`
- Create: `gk-ledger/src/main/java/com/gk/ledger/merchant/service/impl/MerchantAppIdentityServiceImpl.java`

- [ ] **Step 1: Create merchant entities**

Create `MerchantEntity.java`:

```java
package com.gk.ledger.merchant.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.gk.common.core.entity.SimpleEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("merchant")
public class MerchantEntity extends SimpleEntity {
    private Long tenantId;
    private String merchantNo;
    private String merchantName;
    private Integer status;
    private String countryCode;
    private String currency;
    private String remark;
}
```

Create `MerchantAppEntity.java`:

```java
package com.gk.ledger.merchant.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.gk.common.core.entity.SimpleEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("merchant_app")
public class MerchantAppEntity extends SimpleEntity {
    private Long tenantId;
    private Long merchantId;
    private String appId;
    private String appName;
    private String apiSecret;
    private String notifyUrl;
    private String ipWhitelist;
    private Integer status;
    private String remark;
}
```

- [ ] **Step 2: Create DAOs**

Create `MerchantDao.java`:

```java
package com.gk.ledger.merchant.dao;

import com.gk.common.core.dao.BaseDao;
import com.gk.ledger.merchant.entity.MerchantEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MerchantDao extends BaseDao<MerchantEntity> {
}
```

Create `MerchantAppDao.java`:

```java
package com.gk.ledger.merchant.dao;

import com.gk.common.core.dao.BaseDao;
import com.gk.ledger.merchant.entity.MerchantAppEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MerchantAppDao extends BaseDao<MerchantAppEntity> {
}
```

- [ ] **Step 3: Create identity DTO and service**

Create `MerchantAppIdentity.java`:

```java
package com.gk.ledger.merchant.dto;

public record MerchantAppIdentity(
        Long tenantId,
        Long merchantId,
        String appId,
        String apiSecret,
        String notifyUrl,
        String ipWhitelist
) {
}
```

Create `MerchantAppIdentityService.java`:

```java
package com.gk.ledger.merchant.service;

import com.gk.ledger.merchant.dto.MerchantAppIdentity;

public interface MerchantAppIdentityService {
    MerchantAppIdentity getEnabledIdentity(String appId);
}
```

Create `MerchantAppIdentityServiceImpl.java`:

```java
package com.gk.ledger.merchant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gk.ledger.merchant.dao.MerchantAppDao;
import com.gk.ledger.merchant.dto.MerchantAppIdentity;
import com.gk.ledger.merchant.entity.MerchantAppEntity;
import com.gk.ledger.merchant.service.MerchantAppIdentityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MerchantAppIdentityServiceImpl implements MerchantAppIdentityService {
    private final MerchantAppDao merchantAppDao;

    @Override
    public MerchantAppIdentity getEnabledIdentity(String appId) {
        MerchantAppEntity app = merchantAppDao.selectOne(new QueryWrapper<MerchantAppEntity>()
                .eq("app_id", appId)
                .eq("status", 1)
                .last("LIMIT 1"));
        if (app == null) {
            throw new IllegalArgumentException("Merchant app is disabled or does not exist");
        }
        return new MerchantAppIdentity(
                app.getTenantId(),
                app.getMerchantId(),
                app.getAppId(),
                app.getApiSecret(),
                app.getNotifyUrl(),
                app.getIpWhitelist()
        );
    }
}
```

- [ ] **Step 4: Compile**

Run:

```bash
mvn -pl gk-ledger -am test
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 5: Commit**

```bash
git add gk-ledger/src/main/java/com/gk/ledger/merchant
git commit -m "feat: add merchant app identity model"
```

---

### Task 8: Add PSP Configuration Skeleton

**Files:**
- Create: `gk-ledger/src/main/java/com/gk/ledger/psp/entity/PspProviderEntity.java`
- Create: `gk-ledger/src/main/java/com/gk/ledger/psp/entity/PspMethodEntity.java`
- Create: `gk-ledger/src/main/java/com/gk/ledger/psp/entity/PspRouteRuleEntity.java`
- Create: `gk-ledger/src/main/java/com/gk/ledger/psp/dao/PspProviderDao.java`
- Create: `gk-ledger/src/main/java/com/gk/ledger/psp/dao/PspMethodDao.java`
- Create: `gk-ledger/src/main/java/com/gk/ledger/psp/dao/PspRouteRuleDao.java`

- [ ] **Step 1: Create PSP entities**

Create `PspProviderEntity.java`:

```java
package com.gk.ledger.psp.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.gk.common.core.entity.SimpleEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("psp_provider")
public class PspProviderEntity extends SimpleEntity {
    private String pspCode;
    private String pspName;
    private Integer status;
    private String baseUrl;
    private String configJson;
    private String secretJson;
    private String remark;
}
```

Create `PspMethodEntity.java`:

```java
package com.gk.ledger.psp.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.gk.common.core.entity.SimpleEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("psp_method")
public class PspMethodEntity extends SimpleEntity {
    private Long pspId;
    private String methodCode;
    private String pspMethodCode;
    private String countryCode;
    private String currency;
    private String direction;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    private BigDecimal dailyLimit;
    private Integer status;
    private String configJson;
    private String remark;
}
```

Create `PspRouteRuleEntity.java`:

```java
package com.gk.ledger.psp.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.gk.common.core.entity.SimpleEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("psp_route_rule")
public class PspRouteRuleEntity extends SimpleEntity {
    private Long tenantId;
    private Long merchantId;
    private String appId;
    private String countryCode;
    private String currency;
    private String methodCode;
    private String direction;
    private Long pspMethodId;
    private Integer priority;
    private Integer weight;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer status;
    private String remark;
}
```

- [ ] **Step 2: Create PSP DAOs**

Create `PspProviderDao.java`:

```java
package com.gk.ledger.psp.dao;

import com.gk.common.core.dao.BaseDao;
import com.gk.ledger.psp.entity.PspProviderEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PspProviderDao extends BaseDao<PspProviderEntity> {
}
```

Create `PspMethodDao.java`:

```java
package com.gk.ledger.psp.dao;

import com.gk.common.core.dao.BaseDao;
import com.gk.ledger.psp.entity.PspMethodEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PspMethodDao extends BaseDao<PspMethodEntity> {
}
```

Create `PspRouteRuleDao.java`:

```java
package com.gk.ledger.psp.dao;

import com.gk.common.core.dao.BaseDao;
import com.gk.ledger.psp.entity.PspRouteRuleEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PspRouteRuleDao extends BaseDao<PspRouteRuleEntity> {
}
```

- [ ] **Step 3: Compile**

Run:

```bash
mvn -pl gk-ledger -am test
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 4: Commit**

```bash
git add gk-ledger/src/main/java/com/gk/ledger/psp
git commit -m "feat: add psp configuration model"
```

---

### Task 9: Verify Foundation Acceptance Criteria

**Files:**
- Read: `docs/superpowers/specs/2026-06-06-philippines-payment-aggregator-v1-design.md`
- Read: files changed by Tasks 1-8

- [ ] **Step 1: Run the module test suite**

Run:

```bash
mvn -pl gk-ledger -am test
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 2: Verify approved terminology**

Run:

```powershell
Select-String -Path gk-ledger\src\main\java\**\*.java,gk-ledger\src\main\resources\**\* -Pattern 'channel_','provider_pay_url'
```

Expected: no matches.

- [ ] **Step 3: Verify no direct balance mutation appears outside the posting service**

Run:

```powershell
Get-ChildItem -Path gk-ledger\src\main\java -Recurse -Filter *.java | Select-String -Pattern 'setBalance\('
```

Expected: the only match is in `gk-ledger/src/main/java/com/gk/ledger/account/service/impl/LedgerPostingServiceImpl.java`.

- [ ] **Step 4: Verify plan scope is satisfied**

Check that these files exist:

```text
gk-ledger/src/main/resources/sql/v1_ledger_foundation_schema.sql
gk-ledger/src/main/java/com/gk/ledger/account/service/LedgerPostingService.java
gk-ledger/src/main/java/com/gk/ledger/account/service/impl/LedgerPostingServiceImpl.java
gk-ledger/src/main/java/com/gk/ledger/merchant/service/MerchantAppIdentityService.java
gk-ledger/src/main/java/com/gk/ledger/psp/entity/PspProviderEntity.java
```

Expected: all files exist.

- [ ] **Step 5: Commit final verification notes if docs changed**

If no docs changed during verification, do not create a commit. If a plan correction was needed, commit it:

```bash
git add docs/superpowers/plans/2026-06-06-payment-ledger-foundation.md
git commit -m "docs: refine ledger foundation plan"
```
