package com.gk.psp.route;

import com.gk.psp.request.PspOrderRequest;

public interface PspRouteSelector {
    PspRouteResult selectPayin(PspOrderRequest order);

    PspRouteResult selectPayout(PspOrderRequest order);
}
