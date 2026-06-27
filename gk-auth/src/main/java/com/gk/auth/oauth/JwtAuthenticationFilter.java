package com.gk.auth.oauth;

import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.ObjUtil;
import com.gk.auth.config.PublicEndpoints;
import com.gk.auth.entity.SysUser;
import com.gk.auth.service.JpaUserDetailsService;
import com.gk.auth.utils.JwtUtils;
import com.gk.common.constant.Constant;
import com.gk.common.context.ReqContext;
import com.gk.common.context.ReqContextHolder;
import com.alibaba.fastjson2.JSONObject;
import com.gk.common.exception.ErrorCode;
import com.gk.common.exception.GkException;
import com.gk.common.model.R;
import com.gk.common.utils.IpUtils;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JpaUserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JpaUserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return PublicEndpoints.matches(request);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws IOException {
        try {
            authenticate(request);
            filterChain.doFilter(request, response);
        } catch (GkException ex) {
            SecurityContextHolder.clearContext();
            log.warn("JWT auth rejected: code={}, uri={}", ex.getCode(), request.getRequestURI());
            writeAuthError(response, ex);
        } catch (Exception ex) {
            SecurityContextHolder.clearContext();
            log.error("JWT auth failed: uri={}", request.getRequestURI(), ex);
            writeAuthError(response);
        } finally {
            ReqContextHolder.clear();
            MDC.clear();
        }
    }

    private void authenticate(HttpServletRequest request) {
        String jwt = getJwtFromRequest(request);
        if (StringUtils.isBlank(jwt)) {
            throw new GkException(ErrorCode.TOKEN_NOT_EMPTY);
        }

        Claims claims = JwtUtils.parseToken(jwt);
        ReqContext context = formContext(claims, request);
        if (Constant.ADMIN.equals(claims.getSubject()) || Constant.ORG.equals(claims.getSubject())) {
            if (context.getUserId() > 0) {
                SysUser userDetails = userDetailsService.getUserByUserId(Constant.ADMIN, context.getUserId());

                context.setSAdmin(userDetails.isSuperAdmin());
                Boolean jwtSAdmin = claims.get(JwtUtils.SUPER_Admin, Boolean.class);
                if (Boolean.TRUE.equals(jwtSAdmin)) {
                    context.setSAdmin(true);
                }

                Set<Long> roleDeptIds = userDetailsService.getDataScopeList(userDetails.getSubjectId());
                if (roleDeptIds != null && !roleDeptIds.isEmpty()) {
                    context.setDeptIdList(roleDeptIds);
                } else if (ObjUtil.isNotEmpty(userDetails.getDeptId())) {
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

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userDetails, null, authorities);
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } else if (Constant.CLIENT.equals(claims.getSubject())) {
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(claims, null, List.of());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        MDC.put("traceId", context.getTraceId());
        ReqContextHolder.set(context);
    }

    private void writeAuthError(HttpServletResponse response, GkException ex) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(JSONObject.toJSONString(R.errorMsg(ex.getCode(), ex.getMsg())));
    }

    private void writeAuthError(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(JSONObject.toJSONString(R.error(ErrorCode.UNAUTHORIZED)));
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

    public ReqContext formContext(Claims claims, HttpServletRequest request) {
        Long userId = claims.get(JwtUtils.USER_ID, Long.class);
        Long subjectId = claims.get(JwtUtils.SUBJECT_ID, Long.class);
        Long tenantId = claims.get(JwtUtils.TENANT_ID, Long.class);
        Long merchantId = claims.get(JwtUtils.MERCHANT_ID, Long.class);
        Long deptId = claims.get(JwtUtils.DEPT_ID, Long.class);
        Long roleId = claims.get(JwtUtils.ROLE_ID, Long.class);
        List<Long> roleIds = getLongList(claims.get("roleIds", List.class));
        String subjectType = claims.get(JwtUtils.SUBJECT_TYPE, String.class);
        String username = claims.get(JwtUtils.UNAME, String.class);
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
                .userId(userId).subjectId(subjectId).username(username)
                .tenantId(tenantId).merchantId(merchantId).deptId(deptId)
                .roleId(roleId).roleIdList(roleIds).subjectType(subjectType)
                .domain(domain)
                // 请求信息
                .ip(IpUtils.getIpAddr(request))
                .uri(request.getRequestURI())
                .method(request.getMethod())
                .userAgent(request.getHeader(HttpHeaders.USER_AGENT))
                .lang(lang).timezone(timezone).traceId(traceId).build();
    }

    private List<Long> getLongList(List<?> values) {
        if (values == null) {
            return List.of();
        }
        List<Long> result = new ArrayList<>(values.size());
        for (Object value : values) {
            if (value instanceof Number number) {
                result.add(number.longValue());
            } else if (value != null) {
                result.add(Long.parseLong(value.toString()));
            }
        }
        return result;
    }

}
