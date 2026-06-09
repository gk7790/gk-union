package com.gk.openapi.log;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MerchantRequestLogUtilsTest {

    @Test
    void hashesRequestBodyWithSha256() {
        assertEquals(
                "8dd1c9451bed0ccf7dd1fb9073f6dfe3f8eca44e1d1e4af11f8bceef5f7281de",
                MerchantRequestLogUtils.sha256Hex("amount=100.00&app_id=APP001")
        );
    }

    @Test
    void sanitizesSensitiveRequestFields() {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("app_id", "APP001");
        params.put("api_secret", "secret");
        params.put("sign", "abcdef");
        params.put("customer_phone", "+639171234567");
        params.put("customer_email", "user@example.com");
        params.put("amount", "100.00");

        Map<String, Object> sanitized = MerchantRequestLogUtils.sanitizeParams(params);

        assertFalse(sanitized.containsKey("api_secret"));
        assertEquals("***", sanitized.get("sign"));
        assertEquals("+639****4567", sanitized.get("customer_phone"));
        assertEquals("u***@example.com", sanitized.get("customer_email"));
        assertEquals("100.00", sanitized.get("amount"));
        assertTrue(sanitized.containsKey("app_id"));
    }
}
