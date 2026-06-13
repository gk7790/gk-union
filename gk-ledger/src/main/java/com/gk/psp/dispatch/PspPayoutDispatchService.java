package com.gk.psp.dispatch;

import com.gk.payment.payout.entity.PayoutOrderEntity;
import com.gk.psp.route.PspRouteResult;

public interface PspPayoutDispatchService {
    PspPayoutDispatchResult dispatch(PayoutOrderEntity order, PspRouteResult route);
}
