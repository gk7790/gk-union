package com.gk.psp.adapter.world;

import org.apache.commons.lang3.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;

public final class WorldPspSignUtils {
    private WorldPspSignUtils() {
    }

    public static String sign(Map<String, ?> params, String secret) {
        String signText = canonicalText(params) + "&key=" + StringUtils.defaultString(secret);
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(StringUtils.defaultString(secret).getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(signText.getBytes(StandardCharsets.UTF_8))).toLowerCase();
        } catch (Exception ex) {
            throw new IllegalStateException("HmacSHA256 is not available", ex);
        }
    }

    public static boolean verify(Map<String, ?> params, String secret, String signature) {
        if (StringUtils.isBlank(signature)) {
            return false;
        }
        return StringUtils.equalsIgnoreCase(sign(params, secret), signature);
    }

    public static String canonicalText(Map<String, ?> params) {
        if (params == null || params.isEmpty()) {
            return "";
        }
        TreeMap<String, String> sorted = new TreeMap<>();
        params.forEach((key, value) -> {
            if (StringUtils.isBlank(key) || value == null) {
                return;
            }
            String normalizedKey = key.trim();
            if ("sign".equalsIgnoreCase(normalizedKey) || "signature".equalsIgnoreCase(normalizedKey)) {
                return;
            }
            String text = StringUtils.trimToNull(String.valueOf(value));
            if (text != null) {
                sorted.put(normalizedKey, text);
            }
        });
        return sorted.entrySet().stream()
                .map(item -> item.getKey() + "=" + item.getValue())
                .reduce((left, right) -> left + "&" + right)
                .orElse("");
    }
}
