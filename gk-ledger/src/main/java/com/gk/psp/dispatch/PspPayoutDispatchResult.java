package com.gk.psp.dispatch;

import lombok.Data;

@Data
public class PspPayoutDispatchResult {
    private boolean success;
    private String pspRequestNo;
    private String requestUrl;
    private String httpMethod;
    private String requestHeadersJson;
    private String requestBody;
    private Integer responseStatus;
    private String pspOrderNo;
    private String pspMerchantOrderNo;
    private String rawStatus;
    private String responseCode;
    private String responseMessage;
    private String responseSign;
    private String rawResponseJson;
    private String errorCode;
    private String errorMessage;
}
