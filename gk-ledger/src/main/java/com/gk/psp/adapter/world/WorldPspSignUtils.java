package com.gk.psp.adapter.world;

import org.apache.commons.lang3.StringUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * World PSP 扁平参数 MD5 签名，与 OpenAPI Canonical JSON 签名无关。
 */
public final class WorldPspSignUtils {
    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private WorldPspSignUtils() {
    }

    public static String sign(Map<String, ?> params, String secret) {
        return md5Hex(flatSignText(params) + "&key=" + StringUtils.defaultString(secret)).toLowerCase(Locale.ROOT);
    }

    public static boolean verify(Map<String, ?> params, String secret, String signature) {
        if (StringUtils.isBlank(signature)) {
            return false;
        }
        return StringUtils.equalsIgnoreCase(sign(params, secret), signature);
    }

    public static String canonicalText(Map<String, ?> params) {
        return flatSignText(params);
    }

    public static Map<String, Object> withSign(Map<String, Object> params, String secret) {
        Map<String, Object> signed = new LinkedHashMap<>(params);
        signed.put("sign", sign(signed, secret));
        return signed;
    }

    public static String formBody(Map<String, ?> params) {
        if (params == null || params.isEmpty()) {
            return "";
        }
        return params.entrySet().stream()
                .filter(entry -> StringUtils.isNotBlank(entry.getKey()) && entry.getValue() != null)
                .map(entry -> encode(entry.getKey().trim()) + "=" + encode(String.valueOf(entry.getValue())))
                .collect(Collectors.joining("&"));
    }

    private static String flatSignText(Map<String, ?> params) {
        if (params == null || params.isEmpty()) {
            return "";
        }
        TreeMap<String, String> sorted = new TreeMap<>();
        params.forEach((key, value) -> {
            if (StringUtils.isBlank(key) || value == null || "sign".equalsIgnoreCase(key.trim())) {
                return;
            }
            String text = String.valueOf(value).trim();
            if (!text.isEmpty()) {
                sorted.put(key.trim(), text);
            }
        });
        return sorted.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("&"));
    }

    private static String md5Hex(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            char[] result = new char[hash.length * 2];
            for (int i = 0; i < hash.length; i++) {
                int v = hash[i] & 0xff;
                result[i * 2] = HEX[v >>> 4];
                result[i * 2 + 1] = HEX[v & 0x0f];
            }
            return new String(result);
        } catch (Exception ex) {
            throw new IllegalStateException("MD5 sign failed", ex);
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(StringUtils.defaultString(value), StandardCharsets.UTF_8);
    }
}
