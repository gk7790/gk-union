package com.gk.psp.adapter.world;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldPspSignUtilsTest {

    @Test
    void signsParamsWithAsciiOrderAndVerifies() {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("merchant_order_id", "PAY123");
        params.put("amount", "100.00");
        params.put("currency", "PHP");
        params.put("empty", "");

        String signature = WorldPspSignUtils.sign(params, "secret123");

        params.put("sign", signature);
        assertTrue(WorldPspSignUtils.verify(params, "secret123", signature));
        assertFalse(WorldPspSignUtils.verify(params, "wrong-secret", signature));
    }
}
