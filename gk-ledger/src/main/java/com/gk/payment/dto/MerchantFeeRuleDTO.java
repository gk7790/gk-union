package com.gk.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
public class MerchantFeeRuleDTO {
    private Long id;
    @Schema(title = "租户ID")
    private Long tenantId;
    @Schema(title = "租户名称")
    private String tenantName;
    @Schema(title = "商户ID")
    private Long merchantId;
    @Schema(title = "商户名称")
    private String merchantName;
    @Schema(title = "商户应用ID")
    private Long merchantAppId;
    @Schema(title = "商户应用名称")
    private String merchantAppName;
    @Schema(title = "规则名称")
    private String ruleName;
    @Schema(title = "订单类型: PAYIN/PAYOUT")
    private String direction;
    @Schema(title = "国家编码")
    private String countryCode;
    @Schema(title = "币种")
    private String currency;
    @Schema(title = "支付方式")
    private String methodCode;
    @Schema(title = "订单最小金�?)
    private BigDecimal minAmount;
    @Schema(title = "订单最大金�?)
    private BigDecimal maxAmount;
    @Schema(title = "手续费模�? RATE/FIXED/RATE_FIXED")
    private String feeMode;
    @Schema(title = "比例费率")
    private BigDecimal feeRate;
    @Schema(title = "固定手续�?)
    private BigDecimal feeFixed;
    @Schema(title = "最低手续费")
    private BigDecimal minFee;
    @Schema(title = "最高手续费")
    private BigDecimal maxFee;
    @Schema(title = "手续费承担方: MERCHANT/CUSTOMER")
    private String feeBearer;
    @Schema(title = "结算处理方式: DEDUCT/ADD")
    private String settleMode;
    @Schema(title = "优先�?)
    private Integer priority;
    @Schema(title = "生效时间")
    private Instant effectiveAt;
    @Schema(title = "失效时间")
    private Instant expireAt;
    @Schema(title = "状�?)
    private Integer status;
    @Schema(title = "备注")
    private String remark;
    private Long createdBy;
    private Instant createdAt;
    private Long updatedBy;
    private Instant updatedAt;
}
