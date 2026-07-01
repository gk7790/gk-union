package com.gk.psp.dto;

import com.gk.psp.balance.PspBalanceSnap;
import lombok.Data;

import java.time.Instant;

@Data
public class PspAccountDTO {
    private Long id;
    private Long tenantId;
    private Long pspId;
    private String pspAccountNo;
    private String pspAccountName;
    private Integer status;
    private String secretType;
    private String apiKey;
    private String apiSecret;
    private String callbackSecret;
    private String configJson;
    private PspBalanceSnap balanceSnap;
    private String remark;
    private Long createdBy;
    private Instant createdAt;
    private Long updatedBy;
    private Instant updatedAt;
}
