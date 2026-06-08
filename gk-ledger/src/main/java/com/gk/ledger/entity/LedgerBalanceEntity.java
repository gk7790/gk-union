package com.gk.ledger.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.gk.common.core.entity.SimpleEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 账务余额缓存
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("ledger_balance")
public class LedgerBalanceEntity extends SimpleEntity {
    private Long tenantId;
    private Long accountId;
    private String accountNo;
    private String currency;
    private BigDecimal balance;
    private BigDecimal debitTotal;
    private BigDecimal creditTotal;
    @Version
    private Integer version;
    private Long lastEntryId;
    private String lastJournalNo;
    private Instant lastPostedAt;
}
