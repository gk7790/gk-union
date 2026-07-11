package com.gk.api.controller;

import com.gk.common.model.DynMap;
import com.gk.common.redis.PaymentRedisKeys;
import com.gk.common.redis.RedisKeys;
import com.gk.common.redis.RedisUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class DeeplinkTestPageController {
    private final RedisUtils redisUtils;

    @GetMapping(value = "/tools/deeplink-test/{token}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<?> deeplinkTestPage(@PathVariable String token) {
        DynMap params = redisUtils.get(PaymentRedisKeys.getDeeplinkTestTokenKey(token), DynMap.class);
        if (params == null || params.isEmpty()) {
            return ResponseEntity.status(HttpStatus.GONE)
                    .contentType(MediaType.TEXT_HTML)
                    .cacheControl(CacheControl.noStore())
                    .body("""
                            <!doctype html>
                            <html lang="zh-CN">
                            <head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1"><title>链接已失效</title></head>
                            <body style="font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Arial,'Microsoft YaHei',sans-serif;padding:24px;">
                            <h1>链接已失效</h1>
                            <p>这个 App 唤醒测试链接不存在或已经过期，请让内部人员重新生成。</p>
                            </body>
                            </html>
                            """);
        }
        String html = renderHtml(params);
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .cacheControl(CacheControl.noStore())
                .body(html);
    }

    private String renderHtml(DynMap params) {
        try {
            String html = StreamUtils.copyToString(
                    new ClassPathResource("static/tools/deeplink-test.html").getInputStream(),
                    StandardCharsets.UTF_8
            );
            return replaceParams(html, params);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to render deeplink test page", e);
        }
    }

    private String replaceParams(String html, DynMap params) {
        String result = html;
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            result = result.replace("${" + entry.getKey() + "}", escapeHtml(entry.getValue()));
        }
        return result.replaceAll("\\$\\{[A-Za-z0-9_]+}", "");
    }

    private String escapeHtml(Object value) {
        if (value == null) {
            return "";
        }
        return String.valueOf(value)
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#039;");
    }
}
