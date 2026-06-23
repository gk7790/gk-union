package com.gk.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
public class PaymentRouteChannelDTO {
    private Long id;
    @Schema(title = "租户ID")
    private Long tenantId;
    @Schema(title = "路由组ID")
    private Long groupId;
    @Schema(title = "PSP供应商ID")
    private Long pspId;
    @Schema(title = "PSP方法ID")
    private Long pspMethodId;
    @Schema(title = "PSP账户ID")
    private Long pspAccountId;
    @Schema(title = "优先级")
    private Integer priority;
    @Schema(title = "权重")
    private Integer weight;
    @Schema(title = "备用顺序")
    private Integer fallbackOrder;
    @Schema(title = "最小金额")
    private BigDecimal minAmount;
    @Schema(title = "最大金额")
    private BigDecimal maxAmount;
    @Schema(title = "状态")
    private Integer status;
    @Schema(title = "备注")
    private String remark;
    private Long createdBy;
    private Instant createdAt;
    private Long updatedBy;
    private Instant updatedAt;
}
