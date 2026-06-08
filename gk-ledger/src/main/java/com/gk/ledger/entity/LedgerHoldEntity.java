package com.gk.ledger.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.gk.common.core.entity.SimpleEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 账务冻结明细
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("ledger_hold")
public class LedgerHoldEntity extends SimpleEntity {
    private Long tenantId;
    private String holdNo;
    private String ownerType;
    private Long ownerId;
    private String currency;
    private Long availableAccountId;
    private String availableAccountNo;
    private Long frozenAccountId;
    private String frozenAccountNo;
    private String bizType;
    private Long bizId;
    private String bizNo;
    private String holdReason;
    private String holdScope;
    private BigDecimal holdAmount;
    private BigDecimal releasedAmount;
    private BigDecimal consumedAmount;
    private BigDecimal remainingAmount;
    private String status;
    private String holdJournalNo;
    private String lastReleaseJournalNo;
    private String consumeJournalNo;
    private Instant expiredAt;
    private String reason;
    private String remark;
}
