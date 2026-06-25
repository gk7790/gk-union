package com.gk.psp.query;

import com.gk.psp.request.PspOrderRequest;

public interface PspPayQueryService {
    PspOrderQueryResult query(PspOrderRequest order);
}
