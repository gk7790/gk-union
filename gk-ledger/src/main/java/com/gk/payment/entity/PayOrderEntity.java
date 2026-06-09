package com.gk.payment.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.gk.common.core.entity.SimpleEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("pay_order")
public class PayOrderEntity extends SimpleEntity {
    private Long tenantId;
    private Long merchantId;
    private String merchantNo;
    private Long merchantAppId;
    private String appId;
    private String payOrderNo;
    private String merchantOrderNo;
    private String idempotencyKey;
    private String requestId;
    private String orderSource;
    private String countryCode;
    private String currency;
    private String methodCode;
    private BigDecimal amount;
    private BigDecimal paidAmount;
    private BigDecimal merchantFeeAmount;
    private Long merchantFeeRuleId;
    private String merchantFeeSnapshotJson;
    private BigDecimal pspFeeAmount;
    private Long pspFeeRuleId;
    private String pspFeeSnapshotJson;
    private BigDecimal settleAmount;
    private String subject;
    private String description;
    private String clientIp;
    private String payerJson;
    private String notifyUrl;
    private String returnUrl;
    private String status;
    private String statusReason;
    private Instant expireAt;
    private Instant paidAt;
    private Instant closedAt;
    private Instant failedAt;
    private Long routeRuleId;
    private String routeSnapshotJson;
    private Long pspId;
    private String pspCode;
    private Long pspMethodId;
    private String pspMethodCode;
    private Long pspAccountId;
    private String pspAccountNo;
    private String pspRequestNo;
    private String pspOrderNo;
    private String pspStatus;
    private String pspRawStatus;
    private String pspPayUrl;
    private String pspPayParamsJson;
    private String ledgerJournalNo;
    private String settleStatus;
    private Instant settleAt;
    private String settleJournalNo;
    private String outboxEventId;
    private String extraJson;
    private Integer version;
    private String remark;
}
