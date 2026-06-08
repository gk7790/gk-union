package com.gk.ledger.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
public class LedgerBalanceDTO {
    private Long id;
    @Schema(title = "租户ID")
    private Long tenantId;
    @Schema(title = "账户ID")
    private Long accountId;
    @Schema(title = "账户编号")
    private String accountNo;
    @Schema(title = "币种")
    private String currency;
    @Schema(title = "当前余额")
    private BigDecimal balance;
    @Schema(title = "借方累计发生额")
    private BigDecimal debitTotal;
    @Schema(title = "贷方累计发生额")
    private BigDecimal creditTotal;
    @Schema(title = "乐观锁版本号")
    private Integer version;
    @Schema(title = "最后一条分录ID")
    private Long lastEntryId;
    @Schema(title = "最后一张凭证号")
    private String lastJournalNo;
    @Schema(title = "最后过账时间")
    private Instant lastPostedAt;
    private Long createdBy;
    private Instant createdAt;
    private Long updatedBy;
    private Instant updatedAt;
}
