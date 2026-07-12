package com.gk.psp.request;

import lombok.Builder;
import lombok.Data;

/**
 * PSP balance query request boundary object.
 */
@Data
@Builder
public class PspBalanceRequest {
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
