package com.gk.psp.callback.support;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.ByteArrayInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * PSP 回调请求体缓存过滤器 * <p>
 * Servlet 原始 inputStream 默认只能读取一次，PSP 回调既要验签又要解析参数和落日志 * 因此这里提前缓存 body，并包装成可重复读取的请求对象 */
@Component
public class PspCallbackBodyCachingFilter extends OncePerRequestFilter {
    public static final String ATTR_RAW_BODY = PspCallbackBodyCachingFilter.class.getName() + ".RAW_BODY";

    /**
     * 判断当前请求是否需要缓body     * <p>
     * 只处PSP 回调入口，避免影响其他普通接口     */
    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri == null || !uri.startsWith("/psp/callback/");
    }

    /**
     * 缓存请求体并继续执行后续过滤器链     */
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        CachedBodyRequest wrapped = new CachedBodyRequest(request);
        // 原始 body 通过 request attribute 暴露Controller/Service，避免重复读流失败
                wrapped.setAttribute(ATTR_RAW_BODY, new String(wrapped.body, StandardCharsets.UTF_8));
        filterChain.doFilter(wrapped, response);
    }

    /**
     * 支持重复读取 body 的请求包装器     */
    private static class CachedBodyRequest extends HttpServletRequestWrapper {
        private final byte[] body;

        /**
         * 一次性读取原始请求体并保存到内存         */
        CachedBodyRequest(HttpServletRequest request) throws IOException {
            super(request);
            this.body = request.getInputStream().readAllBytes();
        }

        /**
         * 每次调用都基于缓存字节数组创建新的输入流         */
        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(body);
            return new ServletInputStream() {
                @Override
                public boolean isFinished() {
                    return inputStream.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener readListener) {
                    // 当前包装器按同步方式读取 body，不需要异步通知
                                    }

                @Override
                public int read() {
                    return inputStream.read();
                }
            };
        }

        /**
         * 基于可重复读取的输入流创reader         */
        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }
    }
}
