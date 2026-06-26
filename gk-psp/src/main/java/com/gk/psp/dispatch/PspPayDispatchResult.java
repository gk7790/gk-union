package com.gk.psp.dispatch;

import lombok.Data;

@Data
public class PspPayDispatchResult {
    private boolean success;

    /**
     * Platform request number for this PSP call.
     */
    private String pspRequestNo;

    /**
     * Actual PSP request URL.
     */
    private String requestUrl;

    /**
     * Actual PSP HTTP method.
     */
    private String httpMethod;

    /**
     * Sanitized PSP request headers JSON.
     */
    private String requestHeadersJson;

    /**
     * Sanitized PSP request body.
     */
    private String requestBody;

    /**
     * PSP HTTP response status.
     */
    private Integer responseStatus;

    /**
     * Order number returned by the PSP.
     */
    private String pspOrderNo;

    /**
     * Platform order number echoed by the PSP. Usually validated against pay_order_no.
     */
    private String pspMerchantOrderNo;

    /**
     * PSP checkout or payment page URL.
     */
    private String payUrl;

    /**
     * Non-redirect payment payload, such as QR code, form params, or SDK params.
     */
    private String payParamsJson;

    /**
     * Raw PSP business status, such as CREATED, PENDING, or WAITING_PAY.
     */
    private String rawStatus;

    /**
     * Raw PSP response code.
     */
    private String responseCode;

    /**
     * Raw PSP response message.
     */
    private String responseMessage;

    /**
     * PSP response signature, mainly for signature verification troubleshooting.
     */
    private String responseSign;

    /**
     * Raw PSP response JSON.
     */
    private String rawResponseJson;

    /**
     * Normalized platform error code.
     */
    private String errorCode;

    /**
     * Normalized platform error message.
     */
    private String errorMessage;
}
