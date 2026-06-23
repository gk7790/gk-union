package com.gk.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
public class PaymentRouteRuleDTO {
    private Long id;
    @Schema(title = "租户ID")
    private Long tenantId;
    @Schema(title = "路由规则名称")
    private String ruleName;
    @Schema(title = "商户ID，空表示租户级通用规则")
    private Long merchantId;
    @Schema(title = "商户应用ID，空表示不限应用")
    private Long merchantAppId;
    @Schema(title = "交易方向: PAYIN/PAYOUT")
    private String direction;
    @Schema(title = "国家/地区编码")
    private String countryCode;
    @Schema(title = "币种")
    private String currency;
    @Schema(title = "平台统一支付方式编码")
    private String methodCode;
    @Schema(title = "最小金额")
    private BigDecimal minAmount;
    @Schema(title = "最大金额")
    private BigDecimal maxAmount;
    @Schema(title = "命中的路由组ID")
    private Long groupId;
    @Schema(title = "优先级")
    private Integer priority;
    @Schema(title = "生效时间")
    private Instant effectiveAt;
    @Schema(title = "失效时间")
    private Instant expireAt;
    @Schema(title = "状态")
    private Integer status;
    @Schema(title = "备注")
    private String remark;
    private Long createdBy;
    private Instant createdAt;
    private Long updatedBy;
    private Instant updatedAt;
}
