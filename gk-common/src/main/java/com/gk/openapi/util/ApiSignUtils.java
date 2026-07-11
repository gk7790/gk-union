package com.gk.openapi.util;

import com.alibaba.fastjson2.JSON;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;

/**
 * OpenAPI signing utilities.
 * The signature payload is generated from canonical JSON after removing sign
 * fields and empty values. Object keys are sorted by ASCII order; arrays keep
 * their original order after empty elements are removed.
 */
public final class ApiSignUtils {
    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private ApiSignUtils() {
    }

    public static String buildSignText(Map<String, ?> params) {
        if (params == null || params.isEmpty()) {
            return "{}";
        }
        TreeMap<String, Object> payload = new TreeMap<>();
        params.forEach((key, value) -> {
            if (key == null || isSignField(key) || shouldOmitValue(value)) {
                return;
            }
            String normalizedKey = key.trim();
            if (!normalizedKey.isEmpty()) {
                payload.put(normalizedKey, value);
            }
        });
        String json = canonicalJsonFragment(payload);
        return json == null ? "{}" : json;
    }

    public static String createMd5Sign(Map<String, ?> params, String apiSecret) {
        return md5Hex(buildSignText(params) + apiSecret).toLowerCase(Locale.ROOT);
    }

    public static boolean verifyMd5Sign(Map<String, ?> params, String apiSecret, String sign) {
        return secureEquals(createMd5Sign(params, apiSecret), sign == null ? null : sign.toLowerCase(Locale.ROOT));
    }

    public static String createHmacSha256Sign(Map<String, ?> params, String apiSecret) {
        return hmacSha256Hex(buildSignText(params), apiSecret).toLowerCase(Locale.ROOT);
    }

    public static boolean verifyHmacSha256Sign(Map<String, ?> params, String apiSecret, String sign) {
        return secureEquals(createHmacSha256Sign(params, apiSecret), sign == null ? null : sign.toLowerCase(Locale.ROOT));
    }

    public static String md5Hex(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            return toHex(digest.digest(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("MD5 sign failed", ex);
        }
    }

    public static String hmacSha256Hex(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return toHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("HMAC-SHA256 sign failed", ex);
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

    private static boolean isSignField(String key) {
        return "sign".equalsIgnoreCase(key) || "signature".equalsIgnoreCase(key);
    }

    static boolean shouldOmitValue(Object value) {
        switch (value) {
            case null -> {
                return true;
            }
            case CharSequence text -> {
                return text.toString().trim().isEmpty();
            }
            case Map<?, ?> map -> {
                if (map.isEmpty()) {
                    return true;
                }
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    if (entry.getKey() == null) {
                        continue;
                    }
                    if (!shouldOmitValue(entry.getValue())) {
                        return false;
                    }
                }
                return true;
            }
            case Collection<?> collection -> {
                if (collection.isEmpty()) {
                    return true;
                }
                for (Object item : collection) {
                    if (!shouldOmitValue(item)) {
                        return false;
                    }
                }
                return true;
            }
            default -> {
            }
        }
        if (value.getClass().isArray()) {
            int length = java.lang.reflect.Array.getLength(value);
            if (length == 0) {
                return true;
            }
            for (int i = 0; i < length; i++) {
                if (!shouldOmitValue(java.lang.reflect.Array.get(value, i))) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    private static String canonicalJsonFragment(Object value) {
        if (shouldOmitValue(value)) {
            return null;
        }
        if (value instanceof Map<?, ?> map) {
            return canonicalJsonObject(map);
        }
        if (value instanceof Collection<?> collection) {
            return canonicalJsonArray(collection);
        }
        if (value.getClass().isArray()) {
            return canonicalJsonArray(arrayToList(value));
        }
        if (value instanceof CharSequence text) {
            return quote(text.toString().trim());
        }
        if (value instanceof BigDecimal decimal) {
            return decimal.toPlainString();
        }
        if (value instanceof Boolean || value instanceof Number) {
            return String.valueOf(value);
        }
        return quote(String.valueOf(value).trim());
    }

    private static String canonicalJsonObject(Map<?, ?> map) {
        TreeMap<String, Object> sorted = new TreeMap<>();
        map.forEach((key, value) -> {
            if (key == null || shouldOmitValue(value)) {
                return;
            }
            String text = String.valueOf(key).trim();
            if (!text.isEmpty()) {
                sorted.put(text, value);
            }
        });
        if (sorted.isEmpty()) {
            return null;
        }
        StringBuilder builder = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : sorted.entrySet()) {
            String fragment = canonicalJsonFragment(entry.getValue());
            if (fragment == null) {
                continue;
            }
            if (!first) {
                builder.append(',');
            }
            builder.append(quote(entry.getKey())).append(':').append(fragment);
            first = false;
        }
        builder.append('}');
        if (first) {
            return null;
        }
        return builder.toString();
    }

    private static String canonicalJsonArray(Collection<?> values) {
        StringBuilder builder = new StringBuilder("[");
        boolean first = true;
        for (Object value : values) {
            String fragment = canonicalJsonFragment(value);
            if (fragment == null) {
                continue;
            }
            if (!first) {
                builder.append(',');
            }
            builder.append(fragment);
            first = false;
        }
        if (first) {
            return null;
        }
        builder.append(']');
        return builder.toString();
    }

    private static List<Object> arrayToList(Object array) {
        int length = java.lang.reflect.Array.getLength(array);
        List<Object> values = new ArrayList<>(length);
        for (int i = 0; i < length; i++) {
            values.add(java.lang.reflect.Array.get(array, i));
        }
        return values;
    }

    private static String quote(String value) {
        StringBuilder builder = new StringBuilder(value.length() + 2);
        builder.append('"');
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            switch (ch) {
                case '"' -> builder.append("\\\"");
                case '\\' -> builder.append("\\\\");
                case '\b' -> builder.append("\\b");
                case '\f' -> builder.append("\\f");
                case '\n' -> builder.append("\\n");
                case '\r' -> builder.append("\\r");
                case '\t' -> builder.append("\\t");
                default -> {
                    if (ch < 0x20) {
                        builder.append(String.format("\\u%04x", (int) ch));
                    } else {
                        builder.append(ch);
                    }
                }
            }
        }
        builder.append('"');
        return builder.toString();
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
        Map<String, Object> params = new HashMap<>();
        params.put("app_id", "GP4338P2WVC9NZ9F2ZR8NUYFJZZ");
        params.put("timestamp", Instant.now().toEpochMilli() + "");

//        params.put("merchant_order_id", "M2026068101245");
//        params.put("amount", "100.00");
//        params.put("currency", "PHP");
//        params.put("method_code", "MAYA");
//        params.put("notify_url", "https://merchant.example.com/notify");
//        params.put("return_url", "https://merchant.example.com/return");


        params.put("merchant_order_id", "OUT2026068101245");
        params.put("amount", "100.00");
        params.put("currency", "PHP");
        params.put("method_code", "MAYA");
        params.put("notify_url", "https://merchant.example.com/notify");
        params.put("payee", Map.of("account_no", "0454349876543654"));


        String sign = createMd5Sign(params, "e5vuBT7Dd7vwWBqG1-R_-EFKm7ynICE2tIPzeKHFW4w");
        params.put("sign", sign);
        System.out.println(JSON.toJSONString(params));
    }
}
