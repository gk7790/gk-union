package com.gk.ledger.posting;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class MerchantBalanceAdjustPostingRequest {
    private Long tenantId;
    private Long merchantId;
    private String merchantNo;
    private Long merchantAppId;
    private String merchantOrderNo;
    private Long bizId;
    private String adjustOrderNo;
    private String adjustType;
    private String currency;
    private BigDecimal amount;
    private String reverseOfJournalNo;
    private String traceId;
    private String reason;
}
