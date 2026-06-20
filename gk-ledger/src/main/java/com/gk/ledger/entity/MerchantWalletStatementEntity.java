package com.gk.ledger.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.gk.common.core.entity.SimpleEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("merchant_wallet_statement")
public class MerchantWalletStatementEntity extends SimpleEntity {
    private Long tenantId;
    private String statementNo;
    private Long merchantId;
    private String merchantNo;
    private Long merchantAppId;
    private Long journalId;
    private String journalNo;
    private Long entryId;
    private String bizType;
    private Long bizId;
    private String bizNo;
    private String merchantOrderNo;
    private String eventType;
    private Long accountId;
    private String accountNo;
    private String accountType;
    private String currency;
    private String effectType;
    private BigDecimal bizAmount;
    private BigDecimal feeAmount;
    private BigDecimal netAmount;
    private BigDecimal balanceChange;
    private BigDecimal balanceBefore;
    private BigDecimal balanceAfter;
    private String sourceType;
    private String status;
    private Instant postedAt;
    private String traceId;
    private String summary;
    private String remark;
}
