package com.gk.openapi.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gk.openapi.tools.ApiR;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PayOrderResponseTest {

    @Test
    void serializesPublicPayOrderFieldsAndOmitsAccountingFields() throws Exception {
        PayOrderResponse response = new PayOrderResponse();
        response.setPayOrderNo("PAY202606090001");
        response.setMerchantOrderNo("M20260680001");
        response.setStatus("FAILED");
        response.setAmount("100.00");
        response.setCurrency("PHP");

        String json = new ObjectMapper().writeValueAsString(ApiR.success(response));

        assertTrue(json.contains("\"amount\":\"100.00\""));
        assertFalse(json.contains("paidAmount"));
        assertFalse(json.contains("merchantFeeAmount"));
        assertFalse(json.contains("settleAmount"));
        assertFalse(json.contains("payUrl"));
        assertFalse(json.contains("pspOrderNo"));
    }
}
