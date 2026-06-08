package com.gk.ledger.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.gk.common.core.entity.SimpleEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 账务记账凭证
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("ledger_journal")
public class LedgerJournalEntity extends SimpleEntity {
    private Long tenantId;
    private String journalNo;
    private String bizType;
    private Long bizId;
    private String bizNo;
    private String eventType;
    private String currency;
    private BigDecimal totalAmount;
    private Integer entryCount;
    private String idempotencyKey;
    private String status;
    private String reverseOfJournalNo;
    private String reversedByJournalNo;
    private String sourceType;
    private String traceId;
    private Instant postedAt;
    private String remark;
}
