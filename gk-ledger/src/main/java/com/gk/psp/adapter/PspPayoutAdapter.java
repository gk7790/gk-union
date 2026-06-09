package com.gk.psp.adapter;

import com.gk.payment.entity.PayoutOrderEntity;
import com.gk.psp.dispatch.PspPayoutDispatchResult;
import com.gk.psp.route.PspRouteResult;

public interface PspPayoutAdapter {
    boolean supports(String pspCode);

    PspPayoutDispatchResult createPayoutOrder(PayoutOrderEntity order, PspRouteResult route);
}
