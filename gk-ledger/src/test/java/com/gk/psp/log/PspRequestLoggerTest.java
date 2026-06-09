package com.gk.psp.log;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gk.payment.entity.PayOrderEntity;
import com.gk.psp.dispatch.PspPayDispatchResult;
import com.gk.psp.entity.PspRequestLogEntity;
import com.gk.psp.route.PspRouteResult;
import com.gk.psp.service.PspRequestLogService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PspRequestLoggerTest {

    @Test
    void recordsPaySubmitSuccessWithPspResponseSnapshot() {
        PspRequestLogService logService = mock(PspRequestLogService.class);
        PspRequestLogger logger = new PspRequestLogger(logService, new ObjectMapper(), Runnable::run);
        PayOrderEntity order = order();
        PspRouteResult route = route();
        PspPayDispatchResult result = new PspPayDispatchResult();
        result.setSuccess(true);
        result.setPspRequestNo("PRQ202606090001");
        result.setPspOrderNo("PSP202606090001");
        result.setRequestUrl("https://psp.example.com/open-api/create-pay-order");
        result.setHttpMethod("POST");
        result.setResponseStatus(200);
        result.setRawResponseJson("{\"code\":200,\"message\":\"success\"}");

        logger.paySubmitSuccess(order, route, result, 10L);

        ArgumentCaptor<PspRequestLogEntity> captor = ArgumentCaptor.forClass(PspRequestLogEntity.class);
        verify(logService).record(captor.capture());
        PspRequestLogEntity entity = captor.getValue();
        assertNotNull(entity);
        assertEquals("PAY_ORDER", entity.getBizType());
        assertEquals(order.getPayOrderNo(), entity.getBizNo());
        assertEquals("PRQ202606090001", entity.getRequestNo());
        assertEquals("PRQ202606090001", entity.getPspRequestNo());
        assertEquals("PSP202606090001", entity.getPspOrderNo());
        assertEquals("https://psp.example.com/open-api/create-pay-order", entity.getRequestUrl());
        assertEquals(1, entity.getSuccess());
    }

    private PayOrderEntity order() {
        PayOrderEntity order = new PayOrderEntity();
        order.setId(100L);
        order.setTenantId(1L);
        order.setMerchantId(2L);
        order.setPayOrderNo("PAY202606090001");
        order.setMerchantOrderNo("MO202606090001");
        order.setAmount(new BigDecimal("100.00"));
        order.setCurrency("PHP");
        order.setMethodCode("GCASH");
        return order;
    }

    private PspRouteResult route() {
        PspRouteResult route = new PspRouteResult();
        route.setPspId(10L);
        route.setPspCode("DEMO_PSP");
        route.setPspBaseUrl("https://psp.example.com");
        route.setPspMethodCode("GCASH_QR");
        route.setPspAccountNo("ACC001");
        return route;
    }
}
