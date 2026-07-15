package com.gk.auth.oauth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gk.auth.entity.SysUser;
import com.gk.common.model.DynMap;
import com.gk.common.utils.IpUtils;
import com.gk.infra.ipwhitelist.service.SysLoginIpWhitelistService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;
import java.io.InputStream;

public class JsonUsernamePasswordAuthenticationFilter extends UsernamePasswordAuthenticationFilter {
    public static final String LOGIN_USERNAME_ATTR = JsonUsernamePasswordAuthenticationFilter.class.getName() + ".USERNAME";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final SysLoginIpWhitelistService sysLoginIpWhitelistService;

    public JsonUsernamePasswordAuthenticationFilter(SysLoginIpWhitelistService sysLoginIpWhitelistService) {
        this.sysLoginIpWhitelistService = sysLoginIpWhitelistService;
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException {
        if (request.getContentType() != null && request.getContentType().startsWith(MediaType.APPLICATION_JSON_VALUE)) {
            try (InputStream is = request.getInputStream()) {
                DynMap dynMap = objectMapper.readValue(is, DynMap.class);
                String username = dynMap.getStr("username");
                String password = dynMap.getStr("password");
                request.setAttribute(LOGIN_USERNAME_ATTR, username);
                UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(username, password);
                setDetails(request, token);
                Authentication authentication = this.getAuthenticationManager().authenticate(token);
                validateLoginIp(request, authentication);
                return authentication;
            } catch (IOException e) {
                throw new AuthenticationServiceException("Invalid JSON login request");
            }
        }
        request.setAttribute(LOGIN_USERNAME_ATTR, request.getParameter(getUsernameParameter()));
        Authentication authentication = super.attemptAuthentication(request, response);
        validateLoginIp(request, authentication);
        return authentication;
    }

    private void validateLoginIp(HttpServletRequest request, Authentication authentication) {
        if (!(authentication.getPrincipal() instanceof SysUser user)) {
            return;
        }
        String clientIp = IpUtils.getClientIp(request);
        boolean allowed = sysLoginIpWhitelistService.isLoginAllowed(
                user.getSubjectType(),
                user.getTenantId(),
                user.getMerchantId(),
                user.getUserSubjectId(),
                clientIp
        );
        if (!allowed) {
            throw new LockedException("Login IP is not allowed");
        }
    }
}
