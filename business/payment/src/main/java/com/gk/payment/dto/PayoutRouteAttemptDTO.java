package com.gk.payment.dto;

import lombok.Data;

import java.time.Instant;

@Data
public class PayoutRouteAttemptDTO {
    private Long id;
    private Long tenantId;
    private Long payoutOrderId;
    private String payoutOrderNo;
    private Integer attemptNo;
    private Long routeOptionId;
    private Long routeRuleId;
    private Long routeGroupId;
    private Long routeChannelId;
    private Long pspId;
    private String pspCode;
    private Long pspMethodId;
    private String pspMethodCode;
    private Long pspAccountId;
    private String pspAccountNo;
    private String pspBankCode;
    private String submitResultStatus;
    private String errorCode;
    private String errorMessage;
    private String pspRequestNo;
    private String pspOrderNo;
    private String rawStatus;
    private Integer responseStatus;
    private String responseCode;
    private String responseMessage;
    private String rawResponseJson;
    private String remark;
    private Long createdBy;
    private Instant createdAt;
    private Long updatedBy;
    private Instant updatedAt;
}
