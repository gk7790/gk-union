package com.gk.openapi.log;

import org.apache.commons.lang3.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class MerchantRequestLogUtils {
    private static final Set<String> DROP_KEYS = Set.of("api_secret", "secret", "password", "private_key");
    private static final Set<String> MASK_KEYS = Set.of("sign", "signature", "token", "access_token");

    private MerchantRequestLogUtils() {
    }

    public static String sha256Hex(String value) {
        if (value == null) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte item : hash) {
                builder.append(String.format("%02x", item));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }

    public static Map<String, Object> sanitizeParams(Map<String, ?> params) {
        Map<String, Object> sanitized = new LinkedHashMap<>();
        if (params == null || params.isEmpty()) {
            return sanitized;
        }
        params.forEach((key, value) -> {
            String normalizedKey = normalizeKey(key);
            if (DROP_KEYS.contains(normalizedKey) || normalizedKey.contains("secret")) {
                return;
            }
            if (MASK_KEYS.contains(normalizedKey)) {
                sanitized.put(key, "***");
                return;
            }
            sanitized.put(key, sanitizeValue(normalizedKey, value));
        });
        return sanitized;
    }

    public static String maskSignature(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() <= 8) {
            return "***";
        }
        return trimmed.substring(0, 4) + "***" + trimmed.substring(trimmed.length() - 4);
    }

    private static Object sanitizeValue(String normalizedKey, Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value);
        if (StringUtils.isBlank(text)) {
            return value;
        }
        if (normalizedKey.contains("phone")) {
            return maskPhone(text);
        }
        if (normalizedKey.contains("email")) {
            return maskEmail(text);
        }
        if (normalizedKey.contains("account") || normalizedKey.contains("card")) {
            return maskMiddle(text);
        }
        return value;
    }

    private static String maskPhone(String value) {
        if (value.length() <= 8) {
            return "***";
        }
        return value.substring(0, 4) + "****" + value.substring(value.length() - 4);
    }

    private static String maskEmail(String value) {
        int at = value.indexOf('@');
        if (at <= 0) {
            return maskMiddle(value);
        }
        return value.charAt(0) + "***" + value.substring(at);
    }

    private static String maskMiddle(String value) {
        if (value.length() <= 8) {
            return "***";
        }
        return value.substring(0, 4) + "****" + value.substring(value.length() - 4);
    }

    private static String normalizeKey(String key) {
        return StringUtils.defaultString(key).trim().toLowerCase(Locale.ROOT);
    }
}
