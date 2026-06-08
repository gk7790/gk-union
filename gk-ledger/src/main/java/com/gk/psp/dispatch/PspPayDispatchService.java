package com.gk.psp.dispatch;

import com.gk.payment.entity.PayOrderEntity;
import com.gk.psp.route.PspRouteResult;

public interface PspPayDispatchService {
    PspPayDispatchResult dispatch(PayOrderEntity order, PspRouteResult route);
}
