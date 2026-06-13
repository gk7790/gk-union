package com.gk.psp.dispatch.impl;

import com.gk.openapi.error.ApiErrorCode;
import com.gk.openapi.error.ApiException;
import com.gk.payment.entity.PayoutOrderEntity;
import com.gk.psp.adapter.PspPayoutAdapter;
import com.gk.psp.dispatch.PspPayoutDispatchResult;
import com.gk.psp.dispatch.PspPayoutDispatchService;
import com.gk.psp.log.PspRequestLogger;
import com.gk.psp.route.PspRouteResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PspPayoutDispatchServiceImpl implements PspPayoutDispatchService {
    private final List<PspPayoutAdapter> adapters;
    private final PspRequestLogger pspRequestLogger;

    @Override
    public PspPayoutDispatchResult dispatch(PayoutOrderEntity order, PspRouteResult route) {
        long startMs = System.currentTimeMillis();
        try {
            PspPayoutAdapter adapter = adapters.stream()
                    .filter(item -> item.supports(route.getPspCode()))
                    .findFirst()
                    .orElseThrow(() -> new ApiException(ApiErrorCode.SERVICE_NOT_READY, "PSP payout adapter is not configured"));
            PspPayoutDispatchResult result = adapter.createPayoutOrder(order, route);
            pspRequestLogger.payoutSubmitSuccess(order, route, result, System.currentTimeMillis() - startMs);
            return result;
        } catch (RuntimeException ex) {
            pspRequestLogger.payoutSubmitFailed(order, route, ex, System.currentTimeMillis() - startMs);
            throw ex;
        }
    }
}
