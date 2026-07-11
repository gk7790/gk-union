package com.gk.psp.query;

import com.gk.psp.request.PspOrderRequest;

public interface PspPayoutQueryService {
    PspOrderQueryResult query(PspOrderRequest order);
}
