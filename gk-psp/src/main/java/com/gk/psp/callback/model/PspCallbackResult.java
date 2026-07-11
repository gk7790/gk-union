package com.gk.psp.callback.model;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PspCallbackResult {
    private String pspCode;
    private String bizType;
    private String systemOrderNo;
    private String merchantOrderNo;
    private String pspOrderNo;
    private String pspStatus;
    private String orderStatus;
    private BigDecimal amount;
    private String currency;
    private String callbackId;
    private String callbackType;
    private String signature;
    private String errorCode;
    private String errorMessage;
    private String successResponse = "success";
    private String failResponse = "fail";
}
