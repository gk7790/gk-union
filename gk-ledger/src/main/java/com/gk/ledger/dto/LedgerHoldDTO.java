package com.gk.ledger.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Schema(name = "LedgerHoldDTO", description = "冻结记录")
public class LedgerHoldDTO {
    private Long id;
    @Schema(title = "租户ID")
    private Long tenantId;
    @Schema(title = "租户名称")
    private String tenantName;
    @Schema(title = "冻结编号")
    private String holdNo;
    @Schema(title = "资金主体类型")
    private String ownerType;
    @Schema(title = "资金主体ID")
    private Long ownerId;
    @Schema(title = "主体名称")
    private String ownerName;
    @Schema(title = "币种")
    private String currency;
    @Schema(title = "可用账户ID")
    private Long availableAccountId;
    @Schema(title = "可用账户编号")
    private String availableAccountNo;
    @Schema(title = "冻结账户ID")
    private Long frozenAccountId;
    @Schema(title = "冻结账户编号")
    private String frozenAccountNo;
    @Schema(title = "业务类型")
    private String bizType;
    @Schema(title = "业务ID")
    private Long bizId;
    @Schema(title = "业务编号")
    private String bizNo;
    @Schema(title = "冻结原因")
    private String holdReason;
    @Schema(title = "冻结作用")
    private String holdScope;
    @Schema(title = "冻结总金")
    private BigDecimal holdAmount;
    @Schema(title = "已释放金")
    private BigDecimal releasedAmount;
    @Schema(title = "已消耗金")
    private BigDecimal consumedAmount;
    @Schema(title = "剩余冻结金额")
    private BigDecimal remainingAmount;
    @Schema(title = "状")
    private String status;
    @Schema(title = "冻结凭证")
    private String holdJournalNo;
    @Schema(title = "最后释放凭证号")
    private String lastReleaseJournalNo;
    @Schema(title = "消耗凭证号")
    private String consumeJournalNo;
    @Schema(title = "冻结过期时间")
    private Instant expiredAt;
    @Schema(title = "原因说明")
    private String reason;
    @Schema(title = "备注")
    private String remark;
    private Long createdBy;
    private Instant createdAt;
    private Long updatedBy;
    private Instant updatedAt;
}
