package com.gk.payment.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
public class MerchantPayoutOrderDTO {
    private Long id;
    private String merchantNo;
    private Long merchantAppId;
    private String merchantAppName;
    private String appId;
    private String payoutOrderNo;
    private String merchantOrderNo;
    private String requestId;
    private String orderSource;
    private String countryCode;
    private String currency;
    private String methodCode;
    private BigDecimal amount;
    private BigDecimal merchantFeeAmount;
    private BigDecimal totalDebitAmount;
    private String payeeName;
    private String payeeAccountNo;
    private String payeeBankCode;
    private String payeeWalletType;
    private String payeePhone;
    private String payeeEmail;
    private String purpose;
    private String notifyUrl;
    private String merchantNotifyStatus;
    private Instant merchantNotifyAt;
    private String status;
    private String merchantStatusCode;
    private String merchantStatusReason;
    private Instant submittedAt;
    private Instant completedAt;
    private Instant failedAt;
    private Instant cancelledAt;
    private String failCode;
    private String failMsg;
    private String pspCode;
    private Instant createdAt;
    private Instant updatedAt;
}
