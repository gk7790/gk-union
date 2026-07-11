package com.gk.auth.config;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "gk.security")
public class PublicEndpoints {
    private static final List<String> FRAMEWORK_PATTERNS = List.of(
            "/auth/**", "/internal/**", "/public/**", "/static/**",
            "/.well-known/**", "/favicon.ico", "/swagger-ui/**",
            "/swagger-ui.html", "/v3/api-docs/**", "/webjars/**", "/error"
    );

    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private List<String> publicEndpoints = new ArrayList<>();

    public String[] patterns() {
        List<String> patterns = new ArrayList<>(FRAMEWORK_PATTERNS);
        patterns.addAll(publicEndpoints);
        return patterns.toArray(String[]::new);
    }

    public boolean matches(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri == null) return false;
        String contextPath = request.getContextPath();
        if (StringUtils.isNotBlank(contextPath) && uri.startsWith(contextPath)) {
            uri = uri.substring(contextPath.length());
        }
        for (String pattern : patterns()) {
            if (pathMatcher.match(pattern, uri)) return true;
        }
        return false;
    }

    public List<String> getPublicEndpoints() { return publicEndpoints; }
    public void setPublicEndpoints(List<String> publicEndpoints) {
        this.publicEndpoints = publicEndpoints == null ? new ArrayList<>() : new ArrayList<>(publicEndpoints);
    }
}
