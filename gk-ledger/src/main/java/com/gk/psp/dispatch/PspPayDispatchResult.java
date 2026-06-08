package com.gk.psp.dispatch;

import lombok.Data;

@Data
public class PspPayDispatchResult {
    private boolean success;
    private String pspRequestNo;
    private String pspOrderNo;
    private String payUrl;
    private String rawStatus;
    private String errorCode;
    private String errorMessage;
}
