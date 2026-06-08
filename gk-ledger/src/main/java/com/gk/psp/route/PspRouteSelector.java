package com.gk.psp.route;

import com.gk.payment.entity.PayOrderEntity;

public interface PspRouteSelector {
    PspRouteResult selectPayin(PayOrderEntity order);
}
