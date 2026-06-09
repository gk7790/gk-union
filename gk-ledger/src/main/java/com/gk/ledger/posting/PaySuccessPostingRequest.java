package com.gk.ledger.posting;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaySuccessPostingRequest {
    private Long tenantId;
    private Long merchantId;
    private Long bizId;
    private String payOrderNo;
    private String currency;
    private BigDecimal amount;
    private BigDecimal merchantFeeAmount;
    private BigDecimal settleAmount;
    private String traceId;
}
