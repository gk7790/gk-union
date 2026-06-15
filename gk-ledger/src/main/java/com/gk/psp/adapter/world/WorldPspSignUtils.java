package com.gk.psp.adapter.world;

import com.gk.openapi.util.ApiSignUtils;
import org.apache.commons.lang3.StringUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public final class WorldPspSignUtils {
    private WorldPspSignUtils() {
    }

    public static String sign(Map<String, ?> params, String secret) {
        return ApiSignUtils.createMd5Sign(params, secret);
    }

    public static boolean verify(Map<String, ?> params, String secret, String signature) {
        return ApiSignUtils.verifyMd5Sign(params, secret, signature);
    }

    public static String canonicalText(Map<String, ?> params) {
        return ApiSignUtils.buildSortedParamString(params);
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
                .filter(entry -> StringUtils.isNotBlank(entry.getKey()) && StringUtils.isNotBlank(formValue(entry.getValue())))
                .map(entry -> encode(entry.getKey().trim()) + "=" + encode(formValue(entry.getValue())))
                .collect(Collectors.joining("&"));
    }

    private static String formValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof CharSequence text) {
            return text.toString().trim();
        }
        return String.valueOf(value);
    }

    private static String encode(String value) {
        return URLEncoder.encode(StringUtils.defaultString(value), StandardCharsets.UTF_8);
    }
}
