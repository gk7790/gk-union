package com.gk.psp.callback.support;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gk.psp.callback.model.PspCallbackRequest;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 从 HttpServletRequest 构建统一请求对象
 */
@Component
@RequiredArgsConstructor
public class PspCallbackRequestFactory {
    private final ObjectMapper objectMapper;

    public PspCallbackRequest create(String pspCode, String bizType, HttpServletRequest servletRequest, String rawBody) {
        PspCallbackRequest request = new PspCallbackRequest();
        request.setPspCode(StringUtils.defaultString(pspCode).trim().toUpperCase(Locale.ROOT));
        request.setBizType(bizType);
        request.setRawBody(StringUtils.defaultString(rawBody));
        request.setHeaders(headers(servletRequest));
        request.setParams(params(servletRequest, rawBody));
        request.setClientIp(clientIp(servletRequest));
        return request;
    }

    private Map<String, Object> params(HttpServletRequest request, String rawBody) {
        Map<String, Object> params = new LinkedHashMap<>();
        appendFormParams(params, request.getQueryString());
        String body = StringUtils.trimToNull(rawBody);
        if (body == null) {
            return params;
        }
        if (body.startsWith("{")) {
            try {
                params.putAll(objectMapper.readValue(body, new TypeReference<Map<String, Object>>() {
                }));
            } catch (Exception ignored) {
                return params;
            }
            return params;
        }
        appendFormParams(params, body);
        return params;
    }

    private void appendFormParams(Map<String, Object> params, String formText) {
        if (StringUtils.isBlank(formText)) {
            return;
        }
        for (String pair : formText.split("&")) {
            if (StringUtils.isBlank(pair)) {
                continue;
            }
            String[] parts = pair.split("=", 2);
            String key = decode(parts[0]);
            String value = parts.length > 1 ? decode(parts[1]) : "";
            if (StringUtils.isNotBlank(key)) {
                params.put(key, value);
            }
        }
    }

    private Map<String, String> headers(HttpServletRequest request) {
        Map<String, String> headers = new LinkedHashMap<>();
        Enumeration<String> names = request.getHeaderNames();
        while (names != null && names.hasMoreElements()) {
            String name = names.nextElement();
            headers.put(name, request.getHeader(name));
        }
        return headers;
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.isNotBlank(forwarded)) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        return StringUtils.defaultIfBlank(realIp, request.getRemoteAddr());
    }

    private String decode(String value) {
        return URLDecoder.decode(StringUtils.defaultString(value), StandardCharsets.UTF_8);
    }
}
