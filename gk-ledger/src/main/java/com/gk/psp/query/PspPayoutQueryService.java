package com.gk.psp.query;

import com.gk.payment.entity.PayoutOrderEntity;

public interface PspPayoutQueryService {
    PspOrderQueryResult query(PayoutOrderEntity order);
}
