package com.gk.payment.service;

import com.gk.common.core.service.CrudService;
import com.gk.payment.dto.PayoutRouteAttemptDTO;
import com.gk.payment.entity.PayoutRouteAttemptEntity;
import com.gk.payment.entity.PayoutOrderEntity;
import com.gk.psp.dispatch.PspPayoutDispatchResult;
import com.gk.psp.route.PspRouteResult;

public interface PayoutRouteAttemptService extends CrudService<PayoutRouteAttemptEntity, PayoutRouteAttemptDTO> {
    void recordAttempt(PayoutRouteAttemptEntity attempt);

    PayoutRouteAttemptEntity recordAttempt(PayoutOrderEntity order, PspRouteResult route, PspPayoutDispatchResult result);
}
