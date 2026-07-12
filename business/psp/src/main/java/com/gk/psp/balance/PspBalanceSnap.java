package com.gk.psp.balance;

import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
public class PspBalanceSnap {
    public static final String STATUS_NORMAL = "NORMAL";
    public static final String STATUS_LOW = "LOW";
    public static final String STATUS_UNKNOWN = "UNKNOWN";
    public static final String SOURCE_API = "API";
    public static final String SOURCE_SYSTEM = "SYSTEM";

    private Long tenantId;
    private Long pspId;
    private String pspCode;
    private Long pspAccountId;
    private String pspAccountNo;
    private String currency;
    private BigDecimal availableBalance;
    private BigDecimal frozenBalance;
    private BigDecimal totalBalance;
    private String status;
    private String source;
    private Instant updatedAt;
    private Instant expireAt;
    private String errorCode;
    private String errorMessage;
    private String rawResponseJson;
}
