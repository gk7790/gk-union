package com.gk.ledger.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Schema(name = "MerchantWalletStatementDTO", description = "商户钱包流水")
public class MerchantWalletStatementDTO {
    private Long id;
    private Long tenantId;
    private String tenantName;
    private String statementNo;
    private Long merchantId;
    private String merchantNo;
    private String merchantName;
    private Long merchantAppId;
    private String merchantAppName;
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
    private Long createdBy;
    private Instant createdAt;
    private Long updatedBy;
    private Instant updatedAt;
}
