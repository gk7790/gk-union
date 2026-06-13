package com.gk.psp.route;

import com.gk.payment.entity.PayOrderEntity;
import com.gk.payment.payout.entity.PayoutOrderEntity;

public interface PspRouteSelector {
    PspRouteResult selectPayin(PayOrderEntity order);

    PspRouteResult selectPayout(PayoutOrderEntity order);
}
