package com.gk.ledger.posting;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PayoutPostingRequest {
    private Long tenantId;
    private Long merchantId;
    private Long pspAccountId;
    private Long bizId;
    private String payoutOrderNo;
    private String currency;
    private BigDecimal amount;
    private BigDecimal merchantFeeAmount;
    private BigDecimal totalDebitAmount;
    private String traceId;
}
