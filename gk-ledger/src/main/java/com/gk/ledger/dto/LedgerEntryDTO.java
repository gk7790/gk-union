package com.gk.ledger.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Schema(name = "LedgerEntryDTO", description = "账务分录")
public class LedgerEntryDTO {
    private Long id;
    @Schema(title = "租户ID")
    private Long tenantId;
    @Schema(title = "凭证ID")
    private Long journalId;
    @Schema(title = "凭证号")
    private String journalNo;
    @Schema(title = "凭证内分录序号")
    private Integer entryNo;
    @Schema(title = "账户ID")
    private Long accountId;
    @Schema(title = "账户编号")
    private String accountNo;
    @Schema(title = "资金主体类型快照")
    private String ownerType;
    @Schema(title = "资金主体ID快照")
    private Long ownerId;
    @Schema(title = "主体编号")
    private String ownerNo;
    @Schema(title = "主体名称")
    private String ownerName;
    @Schema(title = "主体简称")
    private String ownerShortName;
    @Schema(title = "主体显示名称")
    private String ownerDisplayName;
    @Schema(title = "账户类型快照")
    private String accountType;
    @Schema(title = "币种")
    private String currency;
    @Schema(title = "账户余额方向快照")
    private String normalSide;
    @Schema(title = "记账方向")
    private String direction;
    @Schema(title = "分录金额")
    private BigDecimal amount;
    @Schema(title = "余额变动金额")
    private BigDecimal balanceChange;
    @Schema(title = "变动前余额")
    private BigDecimal balanceBefore;
    @Schema(title = "变动后余额")
    private BigDecimal balanceAfter;
    @Schema(title = "业务类型快照")
    private String bizType;
    @Schema(title = "业务ID快照")
    private Long bizId;
    @Schema(title = "业务编号快照")
    private String bizNo;
    @Schema(title = "业务事件快照")
    private String eventType;
    @Schema(title = "摘要")
    private String summary;
    @Schema(title = "备注")
    private String remark;
    private Long createdBy;
    private Instant createdAt;
    private Long updatedBy;
    private Instant updatedAt;
}
