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
 * PSP 回调请求对象工厂。
 * <p>
 * 负责把原始 {@link HttpServletRequest}、路径中的 PSP 编码和业务类型封装成统一的
 * {@link PspCallbackRequest}，供后续适配器验签、解析和落日志使用。
 */
@Component
@RequiredArgsConstructor
public class PspCallbackRequestFactory {
    private final ObjectMapper objectMapper;

    /**
     * 创建标准 PSP 回调请求对象。
     *
     * @param pspCode 路径或路由中识别出的 PSP 编码
     * @param bizType 业务类型，代收或代付
     * @param servletRequest 原始 HTTP 请求
     * @param rawBody 已缓存的原始请求体
     * @return 标准 PSP 回调请求对象
     */
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

    /**
     * 合并 URL query 参数和 body 参数。
     * <p>
     * JSON body 会解析成 Map；非 JSON body 按 form-urlencoded 方式解析。
     */
    private Map<String, Object> params(HttpServletRequest request, String rawBody) {
        Map<String, Object> params = new LinkedHashMap<>();
        // 先放 query string，再放 body；同名字段以 body 中的值为准。
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
                // body 不是合法 JSON 时保留已解析的 query 参数，后续由适配器或校验流程报错。
                return params;
            }
            return params;
        }
        appendFormParams(params, body);
        return params;
    }

    /**
     * 解析 form-urlencoded 文本到参数 Map。
     */
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

    /**
     * 复制请求头。
     */
    private Map<String, String> headers(HttpServletRequest request) {
        Map<String, String> headers = new LinkedHashMap<>();
        Enumeration<String> names = request.getHeaderNames();
        while (names != null && names.hasMoreElements()) {
            String name = names.nextElement();
            headers.put(name, request.getHeader(name));
        }
        return headers;
    }

    /**
     * 获取客户端 IP。
     * <p>
     * 优先取代理头中的首个 IP，最后回退到 Servlet 容器提供的 remoteAddr。
     */
    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.isNotBlank(forwarded)) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        return StringUtils.defaultIfBlank(realIp, request.getRemoteAddr());
    }

    /**
     * URL 解码单个参数值。
     */
    private String decode(String value) {
        return URLDecoder.decode(StringUtils.defaultString(value), StandardCharsets.UTF_8);
    }
}
