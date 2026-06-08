package com.gk.psp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
public class PspMethodDTO {
    private Long id;
    @Schema(title = "PSP ID")
    private Long pspId;
    @Schema(title = "PSP编码")
    private String pspCode;
    @Schema(title = "平台支付方式编码")
    private String methodCode;
    @Schema(title = "PSP支付方式编码")
    private String pspMethodCode;
    @Schema(title = "支付方式名称")
    private String methodName;
    @Schema(title = "国家编码")
    private String countryCode;
    @Schema(title = "币种")
    private String currency;
    @Schema(title = "方向")
    private String direction;
    @Schema(title = "最小金额")
    private BigDecimal minAmount;
    @Schema(title = "最大金额")
    private BigDecimal maxAmount;
    @Schema(title = "日限额")
    private BigDecimal dailyLimit;
    @Schema(title = "状态")
    private Integer status;
    @Schema(title = "扩展配置JSON")
    private String configJson;
    @Schema(title = "备注")
    private String remark;
    private Long createdBy;
    private Instant createdAt;
    private Long updatedBy;
    private Instant updatedAt;
}
