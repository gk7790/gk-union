package com.gk.psp.adapter;

import com.gk.psp.dispatch.PspPayDispatchResult;
import com.gk.psp.query.PspOrderQueryResult;
import com.gk.psp.request.PspOrderRequest;
import com.gk.psp.route.PspRouteResult;

public interface PspPayAdapter {
    boolean supports(String pspCode);

    PspPayDispatchResult createPayinOrder(PspOrderRequest order, PspRouteResult route);

    PspOrderQueryResult queryPayinOrder(PspOrderRequest order, PspRouteResult route);
}
