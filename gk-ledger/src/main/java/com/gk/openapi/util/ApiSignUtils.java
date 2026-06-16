package com.gk.openapi.util;

import com.alibaba.fastjson2.JSON;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;

public class ApiSignUtils {
    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private ApiSignUtils() {
    }

    public static String hmacSha256Hex(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return toHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("HMAC-SHA256 sign failed", e);
        }
    }

    public static String md5Hex(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            return toHex(digest.digest(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("MD5 sign failed", e);
        }
    }

    public static boolean secureEquals(String expected, String actual) {
        if (expected == null || actual == null) {
            return false;
        }
        byte[] left = expected.getBytes(StandardCharsets.UTF_8);
        byte[] right = actual.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(left, right);
    }

    /**
     * Build the traditional payment-style sign text:
     * key1=value1&key2=value2
     *
     * Rules:
     * - remove sign/signature
     * - remove null and blank string values
     * - sort parameter names by ASCII order
     * - only flat scalar values are supported
     */
    public static String buildSortedParamString(Map<String, ?> params) {
        if (params == null || params.isEmpty()) {
            return "";
        }

        Map<String, String> sorted = new TreeMap<>();
        params.forEach((key, value) -> {
            if (key == null || "sign".equalsIgnoreCase(key) || "signature".equalsIgnoreCase(key)) {
                return;
            }
            String normalized = normalizeParamValue(value);
            if (normalized == null || normalized.isBlank()) {
                return;
            }
            sorted.put(key, normalized);
        });

        List<String> parts = new ArrayList<>(sorted.size());
        sorted.forEach((key, value) -> parts.add(key + "=" + value));
        return String.join("&", parts);
    }

    public static String createHmacSha256Sign(Map<String, ?> params, String apiSecret) {
        String signText = buildSortedParamString(params);
        return hmacSha256Hex(signText, apiSecret).toLowerCase(Locale.ROOT);
    }

    public static boolean verifyHmacSha256Sign(Map<String, ?> params, String apiSecret, String sign) {
        String expected = createHmacSha256Sign(params, apiSecret);
        return secureEquals(expected, sign == null ? null : sign.toLowerCase(Locale.ROOT));
    }

    public static String createMd5Sign(Map<String, ?> params, String apiSecret) {
        String signText = buildSortedParamString(params) + "&key=" + apiSecret;
        return md5Hex(signText).toLowerCase(Locale.ROOT);
    }

    public static boolean verifyMd5Sign(Map<String, ?> params, String apiSecret, String sign) {
        String expected = createMd5Sign(params, apiSecret);
        return secureEquals(expected, sign == null ? null : sign.toLowerCase(Locale.ROOT));
    }

    private static String normalizeParamValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof CharSequence text) {
            return text.toString().trim();
        }
        if (value instanceof BigDecimal decimal) {
            return decimal.toPlainString();
        }
        if (value instanceof Number || value instanceof Boolean) {
            return String.valueOf(value);
        }
        if (value instanceof Map<?, ?> || value instanceof Collection<?> || value.getClass().isArray()) {
            throw new IllegalArgumentException("Nested parameters are not supported for sorted parameter signing");
        }
        return String.valueOf(value).trim();
    }

    private static String toHex(byte[] bytes) {
        char[] result = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xff;
            result[i * 2] = HEX[v >>> 4];
            result[i * 2 + 1] = HEX[v & 0x0f];
        }
        return new String(result);
    }

    public static void main(String[] args) {
        Map<String, String> params = new HashMap<>();
        params.put("app_id", "GP4338P2WVC9NZ9F2ZR8NUYFJZZ");
        params.put("timestamp", Instant.now().toEpochMilli() + "");

        params.put("merchant_order_id", "M20260681012");
        params.put("amount", "100.00");
        params.put("method_code", "GCASH");
        params.put("notify_url", "https://merchant.example.com/notify");
        params.put("return_url", "https://merchant.example.com/return");


        String sign = createMd5Sign(params, "e5vuBT7Dd7vwWBqG1-R_-EFKm7ynICE2tIPzeKHFW4w");
        params.put("sign", sign);
        System.out.println(JSON.toJSONString(params));
    }
}
