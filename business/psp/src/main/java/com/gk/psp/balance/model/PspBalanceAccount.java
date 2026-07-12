package com.gk.psp.balance.model;

import lombok.Data;

@Data
public class PspBalanceAccount {
    private Long tenantId;
    private Long pspId;
    private String pspCode;
    private String pspBaseUrl;
    private String providerConfigJson;
    private Long pspAccountId;
    private String pspAccountNo;
    private String apiKey;
    private String apiSecret;
    private String accountConfigJson;
}
