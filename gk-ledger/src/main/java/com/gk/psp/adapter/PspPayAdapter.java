package com.gk.psp.adapter;

import com.gk.payment.entity.PayOrderEntity;
import com.gk.psp.dispatch.PspPayDispatchResult;
import com.gk.psp.route.PspRouteResult;

public interface PspPayAdapter {
    boolean supports(String pspCode);

    PspPayDispatchResult createPayOrder(PayOrderEntity order, PspRouteResult route);
}
