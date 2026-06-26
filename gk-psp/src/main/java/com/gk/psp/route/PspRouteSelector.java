package com.gk.psp.route;

import com.gk.psp.request.PspOrderRequest;

@SuppressWarnings("unused")
public interface PspRouteSelector {
    PspRouteResult selectPayin(PspOrderRequest order);

    PspRouteResult selectPayout(PspOrderRequest order);
}
