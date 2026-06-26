package com.gk.psp.callback.adapter;

import com.gk.common.model.Result;
import com.gk.psp.callback.model.PspCallbackOrder;
import com.gk.psp.callback.model.PspCallbackRequest;
import com.gk.psp.callback.model.PspCallbackResult;
import com.gk.psp.enums.PspCallbackCurrencyPolicy;

public interface PspCallbackAdapter {
    boolean supports(String pspCode);

    Result<PspCallbackResult> parsePayCallback(PspCallbackRequest request);

    Result<PspCallbackResult> parsePayoutCallback(PspCallbackRequest request);

    Result<Void> verifySign(PspCallbackRequest request, PspCallbackOrder order);

    /**
     * PSP 回调未携currency 时的解析策略，默认回退订单币种     */
    default PspCallbackCurrencyPolicy currencyPolicy() {
        return PspCallbackCurrencyPolicy.ORDER_FALLBACK;
    }
}
