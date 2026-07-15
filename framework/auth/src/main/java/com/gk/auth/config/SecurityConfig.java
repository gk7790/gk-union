package com.gk.auth.config;

import com.alibaba.fastjson2.JSONObject;
import com.gk.common.exception.ErrorCode;
import com.gk.common.model.R;
import com.gk.auth.entity.SysUser;
import com.gk.auth.oauth.JsonUsernamePasswordAuthenticationFilter;
import com.gk.auth.oauth.JwtAuthenticationFilter;
import com.gk.auth.service.JpaUserDetailsService;
import com.gk.auth.service.LoginMfaService;
import com.gk.common.tools.StringFormat;
import com.gk.common.utils.IpUtils;
import com.gk.infra.ipwhitelist.service.SysLoginIpWhitelistService;
import com.gk.infra.notify.NotifyService;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.*;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
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

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    private final JpaUserDetailsService userDetailsService;
    private final SysLoginIpWhitelistService sysLoginIpWhitelistService;
    private final NotifyService notifyService;
    private final LoginMfaService loginMfaService;
    private final PublicEndpoints publicEndpoints;

    public SecurityConfig(JpaUserDetailsService userDetailsService,
                          SysLoginIpWhitelistService sysLoginIpWhitelistService,
                          NotifyService notifyService,
                          LoginMfaService loginMfaService,
                          PublicEndpoints publicEndpoints) {
        this.userDetailsService = userDetailsService;
        this.sysLoginIpWhitelistService = sysLoginIpWhitelistService;
        this.notifyService = notifyService;
        this.loginMfaService = loginMfaService;
        this.publicEndpoints = publicEndpoints;
    }

    /**
     * JSON 登录过滤器
     */
    private JsonUsernamePasswordAuthenticationFilter jsonAuthenticationFilter(AuthenticationManager authManager) {
        JsonUsernamePasswordAuthenticationFilter filter = new JsonUsernamePasswordAuthenticationFilter(sysLoginIpWhitelistService);
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
        DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider(userDetailsService);
        authenticationProvider.setPasswordEncoder(passwordEncoder());
        return new ProviderManager(authenticationProvider);
    }

    /**
     * JWT 认证过滤器
     */
    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(userDetailsService, publicEndpoints);
    }

    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtAuthenticationFilterRegistration(JwtAuthenticationFilter filter) {
        FilterRegistrationBean<JwtAuthenticationFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
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
        return http.authorizeHttpRequests(authorize -> authorize.requestMatchers(publicEndpoints.patterns())
                        .permitAll().anyRequest().authenticated()
                )
                // 禁用CSRF - 前后端分离通常不需要
                .csrf(AbstractHttpConfigurer::disable)
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
            if (loginMfaService.requiresMfa(user)) {
                response.getWriter().write(JSONObject.toJSONString(R.ok(loginMfaService.createChallengeResponse(user))));
                return;
            }

            response.getWriter().write(JSONObject.toJSONString(R.ok(loginMfaService.buildLoginResponse(user))));
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

            String content = StringFormat.format("""
                            ⚠登录失败风险提醒⚠
                            ──────────────
                            入口: {} {}
                            账号: {}
                            IP: {}
                            原因: {}
                            """,
                    request.getMethod(),
                    request.getRequestURI(),
                    request.getAttribute(JsonUsernamePasswordAuthenticationFilter.LOGIN_USERNAME_ATTR),
                    IpUtils.getClientIp(request),
                    getError(exception)
            );
            notifyService.sysWarn(content, "");
        };
    }

    /**
     * 退出成功处理器 - 返回JSON响应
     */
    @Bean
    public LogoutSuccessHandler logoutSuccessHandler() {
        return (request, response, authentication) -> {
            if (authentication != null && authentication.getPrincipal() instanceof SysUser user) {
                userDetailsService.evictLoginCache(user.getId(), user.getUserSubjectId(), user.getDeptId());
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

    // 获取错误信息
    private int getError(Exception exception) {
        return switch (exception) {
            case BadCredentialsException ignored -> ErrorCode.ACCOUNT_PASSWORD_ERROR;
            case DisabledException ignored -> ErrorCode.ACCOUNT_DISABLE;
            case LockedException ignored -> ErrorCode.ACCOUNT_LOCK;
            case AccountExpiredException ignored -> ErrorCode.ACCOUNT_DISABLE;
            case CredentialsExpiredException ignored -> ErrorCode.TOKEN_INVALID;
            default -> ErrorCode.FAILURE;
        };
    }
}
