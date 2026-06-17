package com.gk.openapi.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * OpenAPI 签名工具。
 * <p>
 * 对完整请求体做 Canonical JSON 规范化后再签名，天然支持嵌套对象和数组。
 * <pre>
 *   signText = canonicalJson(params without sign)
 *   MD5:          sign = md5(signText + apiSecret)
 *   HMAC_SHA256:  sign = hmacSha256(signText, apiSecret)
 * </pre>
 * Canonical JSON 规则：
 * <ul>
 *   <li>去掉 sign / signature</li>
 *   <li>去掉 null 字段（递归）</li>
 *   <li>对象 key 递归按 ASCII 排序</li>
 *   <li>数组保持原顺序</li>
 *   <li>紧凑输出，无多余空白</li>
 * </ul>
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
            if (key == null || isSignField(key) || value == null) {
                return;
            }
            String normalizedKey = key.trim();
            if (!normalizedKey.isEmpty()) {
                payload.put(normalizedKey, value);
            }
        });
        return canonicalJson(payload);
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

    public static String canonicalJson(Object value) {
        String json = canonicalJsonFragment(value);
        if (json == null) {
            throw new IllegalArgumentException("Sign payload value cannot be null");
        }
        return json;
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

    private static String canonicalJsonFragment(Object value) {
        if (value == null) {
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
            return quote(text.toString());
        }
        if (value instanceof BigDecimal decimal) {
            return decimal.toPlainString();
        }
        if (value instanceof Boolean || value instanceof Number) {
            return String.valueOf(value);
        }
        return quote(String.valueOf(value));
    }

    private static String canonicalJsonObject(Map<?, ?> map) {
        TreeMap<String, Object> sorted = new TreeMap<>();
        map.forEach((key, value) -> {
            if (key == null || value == null) {
                return;
            }
            String text = String.valueOf(key).trim();
            if (!text.isEmpty()) {
                sorted.put(text, value);
            }
        });
        if (sorted.isEmpty()) {
            return "{}";
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
}
