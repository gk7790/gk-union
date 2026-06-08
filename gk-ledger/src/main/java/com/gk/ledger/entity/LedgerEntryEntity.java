package com.gk.ledger.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.gk.common.core.entity.SimpleEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 账务账变流水/会计分录
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("ledger_entry")
public class LedgerEntryEntity extends SimpleEntity {
    private Long tenantId;
    private Long journalId;
    private String journalNo;
    private Integer entryNo;
    private Long accountId;
    private String accountNo;
    private String ownerType;
    private Long ownerId;
    private String accountType;
    private String currency;
    private String normalSide;
    private String direction;
    private BigDecimal amount;
    private BigDecimal balanceChange;
    private BigDecimal balanceBefore;
    private BigDecimal balanceAfter;
    private String bizType;
    private Long bizId;
    private String bizNo;
    private String eventType;
    private String summary;
    private String remark;
}
