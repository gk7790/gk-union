package com.gk.psp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;

@Data
public class PspRouteRuleDTO {
    private Long id;
    @Schema(title = "租户ID")
    private Long tenantId;
    @Schema(title = "商户ID")
    private Long merchantId;
    @Schema(title = "商户应用ID")
    private Long merchantAppId;
    @Schema(title = "路由规则名称")
    private String routeName;
    @Schema(title = "路由模式: PRIORITY/WEIGHT")
    private String routeMode;
    @Schema(title = "国家编码")
    private String countryCode;
    @Schema(title = "币种")
    private String currency;
    @Schema(title = "平台支付方式编码")
    private String methodCode;
    @Schema(title = "方向")
    private String direction;
    @Schema(title = "PSP ID")
    private Long pspId;
    @Schema(title = "PSP支付方式ID")
    private Long pspMethodId;
    @Schema(title = "PSP账户配置ID")
    private Long pspAccountId;
    @Schema(title = "优先级")
    private Integer priority;
    @Schema(title = "权重")
    private Integer weight;
    @Schema(title = "最小金额")
    private BigDecimal minAmount;
    @Schema(title = "最大金额")
    private BigDecimal maxAmount;
    @Schema(title = "每日开始时间")
    private LocalTime startTime;
    @Schema(title = "每日结束时间")
    private LocalTime endTime;
    @Schema(title = "状态")
    private Integer status;
    @Schema(title = "备注")
    private String remark;
    private Long createdBy;
    private Instant createdAt;
    private Long updatedBy;
    private Instant updatedAt;
}
