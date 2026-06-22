package com.gk.payment.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.gk.common.core.entity.SimpleEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("payout_order")
public class PayoutOrderEntity extends SimpleEntity {
    private Long tenantId;
    private Long merchantId;
    private String merchantNo;
    private Long merchantAppId;
    private String appId;
    private String payoutOrderNo;
    private String merchantOrderNo;
    private String idempotencyKey;
    private String requestId;
    private String orderSource;
    private String countryCode;
    private String currency;
    private String methodCode;
    private BigDecimal amount;
    private BigDecimal merchantFeeAmount;
    private Long merchantFeeRuleId;
    private String merchantFeeSnapshotJson;
    private BigDecimal totalDebitAmount;
    private BigDecimal pspFeeAmount;
    private Long pspFeeRuleId;
    private String pspFeeSnapshotJson;
    private String payeeName;
    private String payeeAccountNo;
    private String payeeBankCode;
    private String payeeWalletType;
    private String payeePhone;
    private String payeeEmail;
    private String payeeJson;
    private String purpose;
    private String notifyUrl;
    private String merchantNotifyStatus;
    private Instant merchantNotifyAt;
    private Long merchantNotifyTaskId;
    private String status;
    private String statusReason;
    private Instant submittedAt;
    private Instant completedAt;
    private Instant failedAt;
    private Instant cancelledAt;
    private String failCode;
    private String failMsg;
    private String holdNo;
    private String freezeJournalNo;
    private String successJournalNo;
    private String releaseJournalNo;
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
    private Instant nextQueryAt;
    private Integer queryCount;
    private String outboxEventId;
    private String extraJson;
    private Integer version;
    private String remark;
}
