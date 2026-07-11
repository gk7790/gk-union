package com.gk.payment.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
public class MerchantPayinOrderDTO {
    private Long id;
    private String merchantNo;
    private Long merchantAppId;
    private String merchantAppName;
    private String appId;
    private String payinOrderNo;
    private String merchantOrderNo;
    private String requestId;
    private String orderSource;
    private String countryCode;
    private String currency;
    private String methodCode;
    private BigDecimal amount;
    private BigDecimal paidAmount;
    private BigDecimal merchantFeeAmount;
    private BigDecimal settleAmount;
    private String subject;
    private String description;
    private String clientIp;
    private String notifyUrl;
    private String returnUrl;
    private String merchantNotifyStatus;
    private Instant merchantNotifyAt;
    private String status;
    private String merchantStatusCode;
    private String merchantStatusReason;
    private Instant expireAt;
    private Instant paidAt;
    private Instant closedAt;
    private Instant failedAt;
    private String pspPayUrl;
    private String pspCode;
    private String settleStatus;
    private Instant settleReleaseAt;
    private Instant settleAt;
    private Instant createdAt;
    private Instant updatedAt;
}
