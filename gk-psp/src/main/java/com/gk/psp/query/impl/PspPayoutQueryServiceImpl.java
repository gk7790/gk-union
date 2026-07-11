package com.gk.psp.query.impl;

import com.gk.common.exception.ErrorCode;
import com.gk.common.exception.GkException;
import com.gk.psp.adapter.PspPayoutAdapter;
import com.gk.psp.log.PspRequestLogger;
import com.gk.psp.query.PspOrderQueryResult;
import com.gk.psp.query.PspPayoutQueryService;
import com.gk.psp.request.PspOrderRequest;
import com.gk.psp.route.PspRouteResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PspPayoutQueryServiceImpl implements PspPayoutQueryService {
    private final List<PspPayoutAdapter> adapters;
    private final PspOrderRouteSnapshot routeSnapshot;
    private final PspRequestLogger pspRequestLogger;

    @Override
    public PspOrderQueryResult query(PspOrderRequest order) {
        long startMs = System.currentTimeMillis();
        PspRouteResult route = routeSnapshot.fromPayoutOrder(order);
        try {
            PspPayoutAdapter adapter = adapters.stream()
                    .filter(item -> item.supports(route.getPspCode()))
                    .findFirst()
                    .orElseThrow(() -> new GkException(ErrorCode.INTERNAL_SERVER_ERROR, "PSP payout query adapter is not configured"));
            PspOrderQueryResult result = adapter.queryPayoutOrder(order, route);
            if (result == null) {
                throw new GkException(ErrorCode.INTERNAL_SERVER_ERROR, "PSP payout query result is empty");
            }
            pspRequestLogger.payoutQuerySuccess(order, route, result, System.currentTimeMillis() - startMs);
            return result;
        } catch (RuntimeException ex) {
            pspRequestLogger.payoutQueryFailed(order, route, ex, System.currentTimeMillis() - startMs);
            throw ex;
        }
    }
}
