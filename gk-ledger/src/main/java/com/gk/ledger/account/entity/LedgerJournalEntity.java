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
