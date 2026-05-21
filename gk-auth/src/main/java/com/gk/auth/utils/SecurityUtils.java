package com.gk.auth.utils;

import cn.hutool.core.util.ObjectUtil;
import com.gk.common.beans.CurrentUser;
import com.gk.common.dto.AuthUser;
import com.gk.common.enums.SysEnum;
import com.gk.auth.entity.SysUser;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Component
public class SecurityUtils implements CurrentUser {

    /**
     * 获取当前用户ID
     */
    public Long getUserId() {
        AuthUser user = getAuthUser();
        return user != null ? user.getId() : null;
    }

    /**
     * 获取当前用户名
     */
    public String getUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null ? authentication.getName() : null;
    }

    /**
     * 获取当前部门ID
     */
    public Long getDeptId() {
        AuthUser user = getAuthUser();
        return user != null ? user.getDeptId() : null;
    }

    /**
     * 获取当前租户ID
     */
    public Long getTenantId() {
        AuthUser user = getAuthUser();
        return user != null ? user.getTenantId() : null;
    }

    /**
     * 获取当前用户权限
     */
    public List<String> getAuthorities() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null) {
            return Collections.emptyList();
        }

        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
    }

    /**
     * 检查是否有权限
     */
    public boolean hasAuthority(String authority) {
        return getAuthorities().contains(authority);
    }

    /**
     * 检查是否有角色
     */
    public boolean hasRole(String role) {
        return hasAuthority("ROLE_" + role);
    }

    /**
     * 是否是管理员
     */
    public boolean isAdmin() {
        AuthUser user = getAuthUser();
        return user.isSAdmin();
    }

    /**
     * 获取当前登录用户
     */
    public AuthUser getAuthUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        if (authentication.getPrincipal() instanceof SysUser principal) {
            return principal.toAuthUser();
        } else if (authentication.getPrincipal() instanceof AuthUser) {
            Object principal = authentication.getPrincipal();
            return (AuthUser) principal;
        } else if (authentication.getPrincipal() instanceof Claims claims) {
            return AuthUser.fromObject(claims);
        }
        return null;
    }

    /**
     * 获取当前登录用户
     */
    public void setDeptAndTenant(Map<String, Object> params) {
        AuthUser user = getAuthUser();
        if (ObjectUtil.isNotEmpty(params) && user.getSAdmin() == SysEnum.sAdmin.NO.value()) {
            Optional<Long> dept = Optional.of(user).map(AuthUser::getDeptId);
            dept.ifPresent(aLong -> params.put("deptId", aLong));
            Optional<Long> tenant = Optional.of(user).map(AuthUser::getTenantId);
            tenant.ifPresent(aLong -> params.put("tenantId", aLong));
        }
    }

    /**
     * 获取当前登录用户
     */
    public void setDept(Map<String, Object> params) {
        AuthUser user = getAuthUser();
        if (ObjectUtil.isNotEmpty(params) && user.getSAdmin() == SysEnum.sAdmin.NO.value()) {
            Optional<Long> dept = Optional.of(user).map(AuthUser::getDeptId);
            dept.ifPresent(aLong -> params.put("deptId", aLong));
        }
    }


}
