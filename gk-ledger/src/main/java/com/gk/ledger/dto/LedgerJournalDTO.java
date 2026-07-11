package com.gk.ledger.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Schema(name = "LedgerJournalDTO", description = "账务凭证")
public class LedgerJournalDTO {
    private Long id;
    @Schema(title = "租户ID")
    private Long tenantId;
    @Schema(title = "租户名称")
    private String tenantName;
    @Schema(title = "凭证")
    private String journalNo;
    @Schema(title = "业务类型")
    private String bizType;
    @Schema(title = "业务ID")
    private Long bizId;
    @Schema(title = "业务编号")
    private String bizNo;
    @Schema(title = "业务事件")
    private String eventType;
    @Schema(title = "币种")
    private String currency;
    @Schema(title = "凭证总金")
    private BigDecimal totalAmount;
    @Schema(title = "分录数量")
    private Integer entryCount;
    @Schema(title = "幂等")
    private String idempotencyKey;
    @Schema(title = "状")
    private String status;
    @Schema(title = "冲正来源凭证")
    private String reverseOfJournalNo;
    @Schema(title = "冲正凭证")
    private String reversedByJournalNo;
    @Schema(title = "记账来源")
    private String sourceType;
    @Schema(title = "链路追踪ID")
    private String traceId;
    @Schema(title = "过账时间")
    private Instant postedAt;
    @Schema(title = "备注")
    private String remark;
    private Long createdBy;
    private Instant createdAt;
    private Long updatedBy;
    private Instant updatedAt;
}
