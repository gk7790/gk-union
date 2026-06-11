package com.gk.openapi.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gk.openapi.dto.PayOrderQueryRequest;
import com.gk.openapi.dto.PayOrderResponse;
import com.gk.openapi.tools.ApiR;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenApiSnakeCaseTest {

    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
        OpenApiJacksonConfig.applySnakeCaseMixIns(builder);
        mapper = builder.build();
    }

    @Test
    void serializesPayOrderResponseWithSnakeCase() throws Exception {
        PayOrderResponse response = new PayOrderResponse();
        response.setSystemOrderId("PAY202606090001");
        response.setMerchantOrderId("M20260680001");
        response.setCountryCode("PH");
        response.setMethodCode("GCASH");
        response.setPayUrl("https://pay.example.com");

        String json = mapper.writeValueAsString(ApiR.success(response));

        assertTrue(json.contains("\"system_order_id\":\"PAY202606090001\""));
        assertTrue(json.contains("\"merchant_order_id\":\"M20260680001\""));
        assertTrue(json.contains("\"country_code\":\"PH\""));
        assertTrue(json.contains("\"method_code\":\"GCASH\""));
        assertTrue(json.contains("\"pay_url\":\"https://pay.example.com\""));
        assertFalse(json.contains("systemOrderId"));
        assertFalse(json.contains("merchantOrderId"));
        assertFalse(json.contains("psp_order_no"));
        assertFalse(json.contains("pspOrderNo"));
    }

    @Test
    void bindsSnakeCaseRequestParams() {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("app_id", "APP001");
        params.put("timestamp", "1780800000000");
        params.put("sign", "abc");
        params.put("merchant_order_id", "M20260681012");

        PayOrderQueryRequest body = mapper.convertValue(params, PayOrderQueryRequest.class);

        assertEquals("M20260681012", body.getMerchantOrderId());
    }

    @Test
    void rejectsCamelCaseBusinessParams() {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("app_id", "APP001");
        params.put("timestamp", "1780800000000");
        params.put("sign", "abc");
        params.put("merchantOrderId", "M20260681012");

        PayOrderQueryRequest body = mapper.convertValue(params, PayOrderQueryRequest.class);

        assertNull(body.getMerchantOrderId());
    }
}
