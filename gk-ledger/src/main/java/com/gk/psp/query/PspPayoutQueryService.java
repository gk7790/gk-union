package com.gk.psp.query;

import com.gk.payment.payout.entity.PayoutOrderEntity;

public interface PspPayoutQueryService {
    PspOrderQueryResult query(PayoutOrderEntity order);
}
