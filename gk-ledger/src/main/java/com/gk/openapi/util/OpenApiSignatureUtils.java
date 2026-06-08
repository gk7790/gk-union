package com.gk.openapi.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import jakarta.servlet.http.HttpServletRequest;

public class OpenApiSignatureUtils {
    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private OpenApiSignatureUtils() {
    }

    public static String sha256Hex(byte[] body) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return toHex(digest.digest(body == null ? new byte[0] : body));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 digest failed", e);
        }
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

    public static String buildSignText(HttpServletRequest request, byte[] body, String timestamp, String nonce) {
        return request.getMethod().toUpperCase(Locale.ROOT) + "\n"
                + request.getRequestURI() + "\n"
                + canonicalQuery(request.getQueryString()) + "\n"
                + timestamp + "\n"
                + nonce + "\n"
                + sha256Hex(body);
    }

    public static boolean secureEquals(String expected, String actual) {
        if (expected == null || actual == null) {
            return false;
        }
        byte[] left = expected.getBytes(StandardCharsets.UTF_8);
        byte[] right = actual.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(left, right);
    }

    public static String canonicalQuery(String queryString) {
        if (queryString == null || queryString.isBlank()) {
            return "";
        }

        Map<String, List<String>> values = new TreeMap<>();
        for (String pair : queryString.split("&")) {
            if (pair.isBlank()) {
                continue;
            }
            int idx = pair.indexOf('=');
            String key = decode(idx >= 0 ? pair.substring(0, idx) : pair);
            String value = decode(idx >= 0 ? pair.substring(idx + 1) : "");
            if ("sign".equalsIgnoreCase(key) || "signature".equalsIgnoreCase(key)) {
                continue;
            }
            values.computeIfAbsent(key, ignored -> new ArrayList<>()).add(value);
        }

        List<String> parts = new ArrayList<>();
        values.forEach((key, itemValues) -> {
            Collections.sort(itemValues);
            for (String value : itemValues) {
                parts.add(key + "=" + value);
            }
        });
        return String.join("&", parts);
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
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
