package com.gk.adjustment.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.gk.common.core.entity.SimpleEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("merchant_balance_adjust_order")
public class MerchantBalanceAdjustOrderEntity extends SimpleEntity {
    private Long tenantId;
    private Long merchantId;
    private String merchantNo;
    private String adjustOrderNo;
    private String adjustType;
    private String sourceType;
    private String currency;
    private BigDecimal amount;
    private String status;
    private String reason;
    private String relatedOrderNo;
    private String reverseOfJournalNo;
    private String ledgerJournalNo;
    private String traceId;
    private Instant postedAt;
    private String operatorType;
    private String operatorId;
    private String extraJson;
    private Integer version;
    private String remark;
}
