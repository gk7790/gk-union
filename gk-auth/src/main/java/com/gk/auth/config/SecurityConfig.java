package com.gk.auth.config;

import com.alibaba.fastjson2.JSONObject;
import com.gk.common.constant.Constant;
import com.gk.common.exception.ErrorCode;
import com.gk.common.model.R;
import com.gk.auth.entity.SysUser;
import com.gk.auth.oauth.JsonUsernamePasswordAuthenticationFilter;
import com.gk.auth.oauth.JwtAuthenticationFilter;
import com.gk.auth.service.JpaUserDetailsService;
import com.gk.auth.utils.JwtUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.*;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    private final JpaUserDetailsService userDetailsService;

    public SecurityConfig(JpaUserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    /**
     * JSON 登录过滤器
     */
    private JsonUsernamePasswordAuthenticationFilter jsonAuthenticationFilter(AuthenticationManager authManager) {
        JsonUsernamePasswordAuthenticationFilter filter = new JsonUsernamePasswordAuthenticationFilter();
        filter.setAuthenticationManager(authManager);
        filter.setFilterProcessesUrl("/auth/login");
        filter.setAuthenticationSuccessHandler(loginSuccessHandler());
        filter.setAuthenticationFailureHandler(loginFailureHandler());
        return filter;
    }


    /**
     * AuthenticationManager Bean
     */
    @Bean
    public AuthenticationManager authenticationManager() {
        return new ProviderManager(
                new DaoAuthenticationProvider() {{
                    setUserDetailsService(userDetailsService);
                    setPasswordEncoder(passwordEncoder());
                }}
        );
    }

    /**
     * JWT 认证过滤器
     */
    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(userDetailsService);
    }

    /**
     * 方法安全表达式处理器: 超级管理员跳过所有 @PreAuthorize 权限校验。
     * <p>必须用 static 方法发布, 以保证早于方法安全配置类初始化。</p>
     */
    @Bean
    static org.springframework.security.access.expression.method.MethodSecurityExpressionHandler methodSecurityExpressionHandler() {
        return new SuperAdminMethodSecurityExpressionHandler();
    }

    @Bean
    @Order(1)
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http, AuthenticationManager authManager) throws Exception {
        // 配置 JSON 登录过滤器
        return http.authorizeHttpRequests(authorize ->authorize.requestMatchers(
                        "/auth/**", // 认证相关端点
                        "/internal/**", // 内部接口使用
                        "/public/**",
                        "/api/v1/**",
                        "/open-api/**", // 商户Api
                        "/psp/callback/**", // PSP回调
                        "/tg/webhook/**", // Telegram入站Webhook(自带secret_token校验)
                        "/static/**",
                        "/.well-known/**", // OIDC发现端点
                        "/favicon.ico",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/v3/api-docs/**",
                        "/webjars/**",
                        "/error"  // 错误端点
                        ).permitAll().anyRequest().authenticated()
                )
                // 禁用CSRF - 前后端分离通常不需要
                .csrf(csrf -> csrf.disable())
                // 启用 CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // 添加 JWT 过滤器（在 JSON 登录过滤器之前）
                .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
                // 添加 JSON 登录过滤器
                .addFilterAt(jsonAuthenticationFilter(authManager), UsernamePasswordAuthenticationFilter.class)
                // 退出登录配置
                .logout(logout -> logout
                        .logoutUrl("/auth/logout")
                        .logoutSuccessHandler(logoutSuccessHandler())
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )
                // 异常处理
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(authenticationEntryPoint())  // 未认证处理
                        .accessDeniedHandler(accessDeniedHandler())  // 权限不足处理
                )
                // 设置为无状态，使用 JWT
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .userDetailsService(userDetailsService).build();
    }

    /**
     * CORS 配置
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*")); // 生产环境请指定域名
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Authorization", "Content-Disposition", "X-Total-Count"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }


    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // ==================== 登录/退出/异常处理 ====================

    /**
     * 登录成功处理器 - 返回JSON响应
     */
    @Bean
    public AuthenticationSuccessHandler loginSuccessHandler() {
        return (request, response, authentication) -> {
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            SysUser user = (SysUser) authentication.getPrincipal();

            // 构建 claims
            Map<String, Object> claims = new HashMap<>();
            claims.put(JwtUtils.USER_ID, user.getId());
            claims.put(JwtUtils.SUBJECT_ID, user.getSubjectId());
            claims.put(JwtUtils.TENANT_ID, user.getTenantId());
            claims.put(JwtUtils.MERCHANT_ID, user.getMerchantId());
            claims.put(JwtUtils.DEPT_ID, user.getDeptId());
            claims.put(JwtUtils.ROLE_ID, user.getRoleId());
            claims.put("roleIds", user.getRoleIdList());
            claims.put(JwtUtils.SUBJECT_TYPE, user.getSubjectType());
            claims.put(JwtUtils.UNAME, user.getUsername());
            claims.put(JwtUtils.SUPER_Admin, user.isSuperAdmin());
            claims.put("email", user.getEmail());
            claims.put("realName", user.getRealName());
            claims.put("roles", user.getRoleList());
            String token = JwtUtils.generateToken(Constant.ADMIN, claims);

            Map<String, Object> userMap = new HashMap<>();
            userMap.put("id", user.getId());
            userMap.put("subjectId", user.getSubjectId());
            userMap.put("username", user.getUsername());
            userMap.put("realName", user.getNickName());
            userMap.put("subjectType", user.getSubjectType());
            userMap.put("tenantId", user.getTenantId());
            userMap.put("merchantId", user.getMerchantId());
            userMap.put("deptId", user.getDeptId());
            userMap.put("roleId", user.getRoleId());
            userMap.put("roleIds", user.getRoleIdList());
            userMap.put("roles", user.getRoleList());
            userMap.put("accessToken", token);
            userMap.put("tokenType", "Bearer");
            userMap.put("expiresIn", 86400);
            response.getWriter().write(JSONObject.toJSONString(R.ok(userMap)));
        };
    }

    /**
     * 登录失败处理器 - 返回JSON响应
     */
    @Bean
    public AuthenticationFailureHandler loginFailureHandler() {
        return (request, response, exception) -> {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(JSONObject.toJSONString(R.error(getError(exception))));
        };
    }

    /**
     * 退出成功处理器 - 返回JSON响应
     */
    @Bean
    public LogoutSuccessHandler logoutSuccessHandler() {
        return (request, response, authentication) -> {
            if (authentication != null && authentication.getPrincipal() instanceof SysUser user) {
                userDetailsService.evictLoginCache(user.getId(), user.getSubjectId(), user.getDeptId());
            }
            SecurityContextHolder.clearContext();
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(JSONObject.toJSONString(R.ok()));
        };
    }

    /**
     * 未认证处理 - 返回JSON响应
     */
    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) -> {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(JSONObject.toJSONString(R.error(ErrorCode.UNAUTHORIZED)));
        };
    }

    /**
     * 权限不足处理 - 返回JSON响应
     */
    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(JSONObject.toJSONString(R.error(ErrorCode.FORBIDDEN)));
        };
    }

    // ==================== 辅助方法 ====================

    // 构建用户信息
    private Map<String, Object> buildUserInfo(Authentication authentication) {
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("username", authentication.getName());
        userInfo.put("authorities", authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList()));

        if (authentication.getPrincipal() instanceof SysUser user) {
            userInfo.put("userId", user.getId());
            userInfo.put("email", user.getEmail());
        }

        return userInfo;
    }

    // 获取错误信息
    private int getError(Exception exception) {
        if (exception instanceof BadCredentialsException) {
            return ErrorCode.ACCOUNT_PASSWORD_ERROR;
        } else if (exception instanceof DisabledException) {
            return ErrorCode.ACCOUNT_DISABLE;
        } else if (exception instanceof LockedException) {
            return ErrorCode.ACCOUNT_LOCK;
        } else if (exception instanceof AccountExpiredException) {
            return ErrorCode.ACCOUNT_DISABLE;
        } else if (exception instanceof CredentialsExpiredException) {
            return ErrorCode.TOKEN_INVALID;
        } else {
            return ErrorCode.FAILURE;
        }
    }
}
