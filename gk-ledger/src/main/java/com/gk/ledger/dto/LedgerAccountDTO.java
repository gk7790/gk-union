package com.gk.ledger.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.Instant;

@Data
@Schema(name = "LedgerAccountDTO", description = "钱包账户")
public class LedgerAccountDTO {
    private Long id;
    @Schema(title = "租户ID")
    private Long tenantId;
    @Schema(title = "账户编号")
    private String accountNo;
    @Schema(title = "资金主体类型")
    private String ownerType;
    @Schema(title = "资金主体ID")
    private Long ownerId;
    @Schema(title = "主体编号")
    private String ownerNo;
    @Schema(title = "主体名称")
    private String ownerName;
    @Schema(title = "主体简称")
    private String ownerShortName;
    @Schema(title = "主体显示名称")
    private String ownerDisplayName;
    @Schema(title = "账户类型")
    private String accountType;
    @Schema(title = "币种")
    private String currency;
    @Schema(title = "账户余额方向")
    private String normalSide;
    @Schema(title = "是否允许负余额")
    private Integer allowNegative;
    @Schema(title = "状态")
    private Integer status;
    @Schema(title = "备注")
    private String remark;
    private Long createdBy;
    private Instant createdAt;
    private Long updatedBy;
    private Instant updatedAt;
}
