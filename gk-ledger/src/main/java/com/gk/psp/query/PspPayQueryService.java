package com.gk.psp.query;

import com.gk.payment.entity.PayOrderEntity;

public interface PspPayQueryService {
    PspOrderQueryResult query(PayOrderEntity order);
}
