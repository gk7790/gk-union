package com.gk.psp.callback.model;

import com.gk.psp.entity.PspCallbackLogEntity;
import lombok.Data;

@Data
public class PspCallbackContext {
    private String pspCode;
    private String bizType;
    private PspCallbackRequest request;
    private PspCallbackResult result;
    private PspCallbackOrder order;
    private PspCallbackLogEntity log;
    private boolean terminal;
    private boolean orderChanged;

    public static PspCallbackContext of(String pspCode, String bizType, PspCallbackRequest request) {
        PspCallbackContext context = new PspCallbackContext();
        context.setPspCode(pspCode);
        context.setBizType(bizType);
        context.setRequest(request);
        return context;
    }
}
