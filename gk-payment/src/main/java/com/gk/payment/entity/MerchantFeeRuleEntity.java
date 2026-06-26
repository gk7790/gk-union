package com.gk.payment.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.gk.common.core.entity.SimpleEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("merchant_fee_rule")
public class MerchantFeeRuleEntity extends SimpleEntity {
    private Long tenantId;
    private Long merchantId;
    private Long merchantAppId;
    private String ruleName;
    private String direction;
    private String countryCode;
    private String currency;
    private String methodCode;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    private String feeMode;
    private BigDecimal feeRate;
    private BigDecimal feeFixed;
    private BigDecimal minFee;
    private BigDecimal maxFee;
    private String feeBearer;
    private String settleMode;
    private Integer priority;
    private Instant effectiveAt;
    private Instant expireAt;
    private Integer status;
    private String remark;
}
