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
