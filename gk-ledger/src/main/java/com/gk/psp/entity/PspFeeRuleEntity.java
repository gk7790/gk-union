package com.gk.psp.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.gk.common.core.entity.SimpleEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("psp_fee_rule")
public class PspFeeRuleEntity extends SimpleEntity {
    private Long tenantId;
    private Long pspId;
    private Long pspAccountId;
    private Long pspMethodId;
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
    private Integer priority;
    private Instant effectiveAt;
    private Instant expireAt;
    private Integer status;
    private String remark;
}
