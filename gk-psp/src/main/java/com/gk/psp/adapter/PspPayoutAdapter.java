package com.gk.psp.adapter;

import com.gk.psp.dispatch.PspPayoutDispatchResult;
import com.gk.psp.query.PspOrderQueryResult;
import com.gk.psp.request.PspOrderRequest;
import com.gk.psp.route.PspRouteResult;

public interface PspPayoutAdapter {
    boolean supports(String pspCode);

    PspPayoutDispatchResult createPayoutOrder(PspOrderRequest order, PspRouteResult route);

    PspOrderQueryResult queryPayoutOrder(PspOrderRequest order, PspRouteResult route);
}
