package com.gk.psp.callback.model;

import lombok.Data;

@Data
public class PspCallbackHandleResult {
    private PspCallbackResponse response;
    private String verifyStatus;
    private String processStatus;
    private boolean orderChanged;
    private boolean terminal;

    public static PspCallbackHandleResult of(PspCallbackResponse response, String verifyStatus,
                                             String processStatus, boolean orderChanged, boolean terminal) {
        PspCallbackHandleResult result = new PspCallbackHandleResult();
        result.setResponse(response);
        result.setVerifyStatus(verifyStatus);
        result.setProcessStatus(processStatus);
        result.setOrderChanged(orderChanged);
        result.setTerminal(terminal);
        return result;
    }
}
