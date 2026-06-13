package com.gk.ledger.adjust.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
public class MerchantBalanceAdjustOrderDTO {
    private Long id;
    @Schema(title = "租户ID")
    private Long tenantId;
    @Schema(title = "商户ID")
    private Long merchantId;
    @Schema(title = "商户号")
    private String merchantNo;
    @Schema(title = "调整单号")
    private String adjustOrderNo;
    @Schema(title = "调整类型: RECHARGE/DEDUCT/REVERSE/SUPPLEMENT")
    private String adjustType;
    @Schema(title = "来源: MANUAL/SYSTEM/API")
    private String sourceType;
    @Schema(title = "币种")
    private String currency;
    @Schema(title = "金额")
    private BigDecimal amount;
    @Schema(title = "状态")
    private String status;
    @Schema(title = "原因")
    private String reason;
    @Schema(title = "关联业务单号")
    private String relatedOrderNo;
    @Schema(title = "冲正来源凭证号")
    private String reverseOfJournalNo;
    @Schema(title = "账本凭证号")
    private String ledgerJournalNo;
    @Schema(title = "链路追踪ID")
    private String traceId;
    @Schema(title = "入账时间")
    private Instant postedAt;
    @Schema(title = "操作方类型")
    private String operatorType;
    @Schema(title = "操作方ID")
    private String operatorId;
    @Schema(title = "扩展JSON")
    private String extraJson;
    private Integer version;
    @Schema(title = "备注")
    private String remark;
    private Long createdBy;
    private Instant createdAt;
    private Long updatedBy;
    private Instant updatedAt;
}
