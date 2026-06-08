package com.gk.auth.oauth;

import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.ObjUtil;
import com.gk.auth.entity.SysUser;
import com.gk.auth.service.JpaUserDetailsService;
import com.gk.auth.utils.JwtUtils;
import com.gk.common.constant.Constant;
import com.gk.common.context.ReqContext;
import com.gk.common.context.ReqContextHolder;
import com.gk.common.utils.IpUtils;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JpaUserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JpaUserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri != null && uri.startsWith("/api/v1/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws IOException {
        try {
            // 获取 Token
            String jwt = getJwtFromRequest(request);
            // 验证 Token
            if (StringUtils.isNotBlank(jwt) && JwtUtils.validateToken(jwt)) {
                Claims claims = JwtUtils.parseToken(jwt);

                ReqContext context = formContext(claims, request);

                if (Constant.ADMIN.equals(claims.getSubject()) || Constant.ORG.equals(claims.getSubject())) {
                    // 确保用户未认证且用户名有效
                    if (context.getUserId() > 0) {

                        // 加载用户信息
                        SysUser userDetails = userDetailsService.getUserByUserId(Constant.ADMIN, context.getUserId());

                        context.setSAdmin(userDetails.isSuperAdmin());

                        if (ObjUtil.isNotEmpty(userDetails.getDeptId())) {
                            Set<Long> subDeptIdList = userDetailsService.getSubDeptIdList(userDetails.getDeptId());
                            context.setDeptIdList(subDeptIdList);
                        }

                        Set<String> authList = Optional.of(userDetails).map(SysUser::getAuthList).orElse(Set.of());
                        Set<String> roleList = Optional.of(userDetails).map(SysUser::getRoleList).orElse(Set.of());

                        List<SimpleGrantedAuthority> authorities = new ArrayList<>(roleList.size() + authList.size());
                        for (String auth : roleList) {
                            authorities.add(new SimpleGrantedAuthority("ROLE_" + auth));
                        }
                        for (String permission : authList) {
                            authorities.add(new SimpleGrantedAuthority(permission));
                        }

                        // 创建认证对象
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(userDetails, null, authorities);

                        // 设置认证详情
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                        // 设置到 SecurityContext
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                } else if (Constant.CLIENT.equals(claims.getSubject())) {
                    // 创建认证对象
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(claims, null, List.of());
                    // 设置认证详情
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // 设置到 SecurityContext
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
                // ========== 3. MDC（日志追踪强烈建议） ==========
                MDC.put("traceId", context.getTraceId());
                ReqContextHolder.set(context);
            }
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            log.error("JWT 认证失败: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        } finally {
            // ========== 4. 清理上下文（必须） ==========
            ReqContextHolder.clear();
            MDC.clear();
        }
    }

    /**
     * 从请求中提取 JWT Token
     */
    private String getJwtFromRequest(HttpServletRequest request) {
        // 1. 从 Authorization Header 获取
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.isNotBlank(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        // 2. 从 URL 参数获取（可选）
        String tokenParam = request.getParameter("token");
        if (StringUtils.isNotBlank(tokenParam)) {
            return tokenParam;
        }

        return null;
    }

    /**
     * 从 Token 中提取权限信息
     */
    private List<SimpleGrantedAuthority> getAuthoritiesFromToken(String token) {
        try {
            List<String> authorities = JwtUtils.getAuthoritiesFromToken(token);
            if (authorities != null) {
                return authorities.stream()
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.warn("从 Token 中提取权限失败: {}", e.getMessage());
        }
        return Collections.emptyList();
    }


    public ReqContext formContext(Claims claims, HttpServletRequest request) {
        Long userId = claims.get(JwtUtils.USER_ID, Long.class);
        Long tenantId = claims.get(JwtUtils.TENANT_ID, Long.class);
        Long deptId = claims.get(JwtUtils.DEPT_ID, Long.class);
        String username = claims.get(JwtUtils.UNAME, String.class);
        Integer scope = claims.get(JwtUtils.SCOPE, Integer.class);
        Integer domain = claims.get(JwtUtils.DOMAIN, Integer.class);

        if (ObjUtil.isEmpty(userId)) {
            userId = 0L;
        }

        String lang = request.getHeader("Accept-Language");
        String timezone = request.getHeader("X-Timezone");
        String traceId = request.getHeader("X-Trace-Id");
        if (StringUtils.isBlank(traceId)) {
            traceId = UUID.fastUUID().toString();
        }

        return ReqContext.builder().model(claims.getSubject())
                // 用户信息
                .userId(userId).username(username).tenantId(tenantId).deptId(deptId)
                // 账户领域和业务员领域
                .scope(scope).domain(domain)
                // 请求信息
                .ip(IpUtils.getIpAddr(request))
                .uri(request.getRequestURI())
                .method(request.getMethod())
                .userAgent(request.getHeader(HttpHeaders.USER_AGENT))
                .lang(lang).timezone(timezone).traceId(traceId).build();
    }

}
