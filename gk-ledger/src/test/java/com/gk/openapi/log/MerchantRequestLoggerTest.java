package com.gk.openapi.log;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gk.openapi.dto.PayOrderCreateRequest;
import com.gk.openapi.dto.PayOrderQueryRequest;
import com.gk.openapi.dto.PayOrderResponse;
import com.gk.openapi.security.ApiReqContext;
import com.gk.openapi.security.ApiReqContextHolder;
import com.gk.openapi.security.OpenApiAuthFilter;
import com.gk.openapi.tools.ApiR;
import com.gk.payment.entity.MerchantRequestLogEntity;
import com.gk.payment.service.MerchantRequestLogService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class MerchantRequestLoggerTest {

    @AfterEach
    void tearDown() {
        ApiReqContextHolder.clear();
    }

    @Test
    void recordsPayCreateSuccessWithShortFacadeMethod() {
        MerchantRequestLogService logService = mock(MerchantRequestLogService.class);
        MerchantRequestLogger logger = new MerchantRequestLogger(logService, new ObjectMapper(), Runnable::run);
        MockHttpServletRequest request = requestWithSignParams();
        ApiReqContextHolder.set(ApiReqContext.builder()
                .tenantId(1L)
                .merchantId(2L)
                .merchantNo("M202606090001")
                .merchantAppId(3L)
                .appId("APP202606090001")
                .clientIp("127.0.0.1")
                .traceId("trace-001")
                .build());

        PayOrderCreateRequest body = new PayOrderCreateRequest();
        body.setMerchantOrderNo("MO202606090001");
        PayOrderResponse orderResp = new PayOrderResponse();
        orderResp.setPayOrderNo("PAY202606090001");
        orderResp.setMerchantOrderNo(body.getMerchantOrderNo());

        logger.payCreateSuccess(request, body, orderResp, ApiR.success(orderResp), System.currentTimeMillis());

        ArgumentCaptor<MerchantRequestLogEntity> captor = ArgumentCaptor.forClass(MerchantRequestLogEntity.class);
        verify(logService).record(captor.capture());
        MerchantRequestLogEntity entity = captor.getValue();
        assertNotNull(entity);
        assertEquals("PAY_ORDER", entity.getBizType());
        assertEquals("创建代收订单", entity.getApiName());
        assertEquals("MO202606090001", entity.getMerchantOrderNo());
        assertEquals("PAY202606090001", entity.getBizNo());
        assertEquals("SUCCESS", entity.getStatus());
        assertEquals(1, entity.getSignValid());
    }

    @Test
    void recordsPayQuerySuccessWithShortFacadeMethod() {
        MerchantRequestLogService logService = mock(MerchantRequestLogService.class);
        MerchantRequestLogger logger = new MerchantRequestLogger(logService, new ObjectMapper(), Runnable::run);
        MockHttpServletRequest request = requestWithSignParams();
        ApiReqContextHolder.set(ApiReqContext.builder()
                .tenantId(1L)
                .merchantId(2L)
                .merchantNo("M202606090001")
                .merchantAppId(3L)
                .appId("APP202606090001")
                .clientIp("127.0.0.1")
                .traceId("trace-001")
                .build());

        PayOrderQueryRequest body = new PayOrderQueryRequest();
        body.setMerchantOrderNo("MO202606090001");
        PayOrderResponse orderResp = new PayOrderResponse();
        orderResp.setPayOrderNo("PAY202606090001");
        orderResp.setMerchantOrderNo(body.getMerchantOrderNo());

        logger.payQuerySuccess(request, body, orderResp, ApiR.success(orderResp), System.currentTimeMillis());

        ArgumentCaptor<MerchantRequestLogEntity> captor = ArgumentCaptor.forClass(MerchantRequestLogEntity.class);
        verify(logService).record(captor.capture());
        MerchantRequestLogEntity entity = captor.getValue();
        assertNotNull(entity);
        assertEquals("PAY_ORDER", entity.getBizType());
        assertEquals("查询代收订单", entity.getApiName());
        assertEquals("MO202606090001", entity.getMerchantOrderNo());
        assertEquals("PAY202606090001", entity.getBizNo());
        assertEquals("SUCCESS", entity.getStatus());
    }

    private MockHttpServletRequest requestWithSignParams() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/pay/create");
        request.addHeader("User-Agent", "JUnit");
        request.setRemoteAddr("127.0.0.1");
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("app_id", "APP202606090001");
        params.put("timestamp", "1780912690325");
        params.put("sign_type", "HMAC_SHA256");
        params.put("sign", "abcdef123456");
        request.setAttribute(OpenApiAuthFilter.ATTR_SIGN_PARAMS, params);
        return request;
    }
}
