package com.gk.psp.route;

import lombok.Data;

@Data
public class PspRouteResult {
    private Long routeRuleId;

    private Long pspId;
    private String pspCode;
    private String pspBaseUrl;
    private String providerConfigJson;
    private String pspCallbackUrl;

    private Long pspMethodId;
    private String pspMethodCode;
    private String methodConfigJson;

    private Long pspAccountId;
    private String pspAccountNo;
    private String pspAccountApiKey;
    private String pspAccountApiSecret;
    private String accountConfigJson;
}
