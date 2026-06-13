package com.gk.psp.query.impl;

import com.gk.openapi.error.ApiErrorCode;
import com.gk.openapi.error.ApiException;
import com.gk.payment.entity.PayOrderEntity;
import com.gk.psp.adapter.PspPayAdapter;
import com.gk.psp.log.PspRequestLogger;
import com.gk.psp.query.PspOrderQueryResult;
import com.gk.psp.query.PspPayQueryService;
import com.gk.psp.route.PspRouteResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PspPayQueryServiceImpl implements PspPayQueryService {
    private final List<PspPayAdapter> adapters;
    private final PspOrderRouteSnapshot routeSnapshot;
    private final PspRequestLogger pspRequestLogger;

    @Override
    public PspOrderQueryResult query(PayOrderEntity order) {
        long startMs = System.currentTimeMillis();
        PspRouteResult route = routeSnapshot.fromPayOrder(order);
        try {
            PspPayAdapter adapter = adapters.stream()
                    .filter(item -> item.supports(route.getPspCode()))
                    .findFirst()
                    .orElseThrow(() -> new ApiException(ApiErrorCode.SERVICE_NOT_READY, "PSP pay query adapter is not configured"));
            PspOrderQueryResult result = adapter.queryPayOrder(order, route);
            if (result == null) {
                throw new ApiException(ApiErrorCode.SYSTEM_ERROR, "PSP pay query result is empty");
            }
            pspRequestLogger.payQuerySuccess(order, route, result, System.currentTimeMillis() - startMs);
            return result;
        } catch (RuntimeException ex) {
            pspRequestLogger.payQueryFailed(order, route, ex, System.currentTimeMillis() - startMs);
            throw ex;
        }
    }
}
