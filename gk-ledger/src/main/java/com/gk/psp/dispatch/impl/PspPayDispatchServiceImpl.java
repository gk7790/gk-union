package com.gk.psp.dispatch.impl;

import com.gk.openapi.error.ApiErrorCode;
import com.gk.openapi.error.ApiException;
import com.gk.payment.entity.PayOrderEntity;
import com.gk.psp.adapter.PspPayAdapter;
import com.gk.psp.dispatch.PspPayDispatchResult;
import com.gk.psp.dispatch.PspPayDispatchService;
import com.gk.psp.log.PspRequestLogger;
import com.gk.psp.route.PspRouteResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PspPayDispatchServiceImpl implements PspPayDispatchService {
    private final List<PspPayAdapter> adapters;
    private final PspRequestLogger pspRequestLogger;

    @Override
    public PspPayDispatchResult dispatch(PayOrderEntity order, PspRouteResult route) {
        long startMs = System.currentTimeMillis();
        try {
            PspPayAdapter adapter = adapters.stream()
                    .filter(item -> item.supports(route.getPspCode()))
                    .findFirst()
                    .orElseThrow(() -> new ApiException(ApiErrorCode.SERVICE_NOT_READY, "PSP adapter is not configured"));
            PspPayDispatchResult result = adapter.createPayOrder(order, route);
            pspRequestLogger.paySubmitSuccess(order, route, result, System.currentTimeMillis() - startMs);
            return result;
        } catch (RuntimeException ex) {
            pspRequestLogger.paySubmitFailed(order, route, ex, System.currentTimeMillis() - startMs);
            throw ex;
        }
    }
}
