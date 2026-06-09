package com.gk.psp.callback.adapter;

import com.gk.psp.callback.model.PspCallbackRequest;
import com.gk.psp.callback.model.PspCallbackResult;

public interface PspCallbackAdapter {
    boolean supports(String pspCode);

    PspCallbackResult parsePayCallback(PspCallbackRequest request);

    PspCallbackResult parsePayoutCallback(PspCallbackRequest request);

    boolean verifySign(PspCallbackRequest request);
}
