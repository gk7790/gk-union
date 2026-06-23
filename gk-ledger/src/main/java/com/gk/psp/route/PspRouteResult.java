package com.gk.psp.route;

import lombok.Data;

@Data
public class PspRouteResult {
    private Long routeRuleId;
    private Long routeGroupId;
    private Long routeChannelId;

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

    /**
     * PSP 侧银行编码。
     * <p>
     * 代付银行卡场景中，订单保存平台统一银行编码，提交 PSP 前转换为 PSP 银行编码。
     */
    private String pspBankCode;
}
