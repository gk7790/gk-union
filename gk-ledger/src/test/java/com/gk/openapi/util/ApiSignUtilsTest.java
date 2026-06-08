package com.gk.openapi.util;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ApiSignUtilsTest {

    public static void createsAsciiSortedHmacSha256Signature() {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("sign", "should-not-join");
        params.put("merchant_order_id", "M202606080001");
        params.put("amount", "100.00");
        params.put("empty", "");
        params.put("pay_channel", "GCASH");
        params.put("notify_url", "https://merchant.example.com/notify");
        params.put("page_return_url", "https://merchant.example.com/return");
        params.put("app_id", "APP_PH_MANILA_001");
        params.put("timestamp", "1780800000000");
        params.put("nonce", "9f8a7c6d");

        String signText = ApiSignUtils.buildSortedParamString(params);
        String sign = ApiSignUtils.createHmacSha256Sign(params, "secret_123456");

        assertEquals("amount=100.00&app_id=APP_PH_MANILA_001&merchant_order_id=M202606080001&nonce=9f8a7c6d&notify_url=https://merchant.example.com/notify&page_return_url=https://merchant.example.com/return&pay_channel=GCASH&timestamp=1780800000000", signText);
        assertEquals("11bbe9c4568bd75008b1d03436ed3dafb271deabc851a38e5598ba5d6b8cda8b", sign);
        assertTrue(ApiSignUtils.verifyHmacSha256Sign(params, "secret_123456", sign));
    }

    public static void createsTraditionalMd5Signature() {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("sign", "should-not-join");
        params.put("merchant_order_id", "M202606080001");
        params.put("amount", "100.00");
        params.put("pay_channel", "GCASH");
        params.put("notify_url", "https://merchant.example.com/notify");
        params.put("page_return_url", "https://merchant.example.com/return");
        params.put("app_id", "G87QQS3WPWQ7EKTXE79FG57N3CX");
        params.put("timestamp", Instant.now().toEpochMilli());

        String sign = ApiSignUtils.createMd5Sign(params, "94K-hU41SvABlROfdMDd-VpAi5SyZVDFjKecJzqmFj8");

        params.put("sign", sign);

        System.out.println(JSONObject.toJSONString(params));
        System.out.println(ApiSignUtils.verifyMd5Sign(params, "94K-hU41SvABlROfdMDd-VpAi5SyZVDFjKecJzqmFj8", sign));
    }

    public static void yueMd5Signature() {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("app_id", "G87QQS3WPWQ7EKTXE79FG57N3CX");
        params.put("timestamp", Instant.now().toEpochMilli());

        String sign = ApiSignUtils.createMd5Sign(params, "94K-hU41SvABlROfdMDd-VpAi5SyZVDFjKecJzqmFj8");

        params.put("sign", sign);

        System.out.println("余额: " + JSONObject.toJSONString(params));
    }

    public static void main(String[] args) {
        yueMd5Signature();
        System.out.println(IdWorker.getId());
    }
}
