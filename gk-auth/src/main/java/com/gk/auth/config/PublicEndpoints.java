package com.gk.auth.config;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.AntPathMatcher;

public final class PublicEndpoints {
    public static final String[] PATTERNS = {
            "/auth/**",
            "/internal/**",
            "/public/**",
            "/api/v1/**",
            "/open-api/**",
            "/psp/callback/**",
            "/tg/webhook/**",
            "/static/**",
            "/.well-known/**",
            "/favicon.ico",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**",
            "/webjars/**",
            "/error"
    };

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private PublicEndpoints() {
    }

    public static boolean matches(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri == null) {
            return false;
        }

        String contextPath = request.getContextPath();
        if (StringUtils.isNotBlank(contextPath) && uri.startsWith(contextPath)) {
            uri = uri.substring(contextPath.length());
        }

        for (String pattern : PATTERNS) {
            if (PATH_MATCHER.match(pattern, uri)) {
                return true;
            }
        }
        return false;
    }
}
