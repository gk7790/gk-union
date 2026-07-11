package com.gk.auth.config;

import com.gk.auth.entity.SysUser;
import org.springframework.security.access.expression.method.MethodSecurityExpressionOperations;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

/**
 * 包装默认的方法安全表达式根: 当前用户为超级管理员时, 所有 hasAuthority / hasAnyAuthority /
 * hasRole / hasAnyRole / hasPermission 一律放行(不再校验具体权限码), 其余委托给默认实现。
 * <p>
 * 这样控制器上只需写 {@code @PreAuthorize("hasAuthority('xxx')")}, 无需每处都追加
 * {@code or hasAnyRole('sadmin')}。
 */
public class SuperAdminSecurityExpressionRoot implements MethodSecurityExpressionOperations {
    /** 超级管理员角色权威字符串(角色码 sadmin 经 ROLE_ 前缀拼装) */
    public static final String SUPER_ADMIN_AUTHORITY = "ROLE_sadmin";

    private final MethodSecurityExpressionOperations delegate;

    public SuperAdminSecurityExpressionRoot(MethodSecurityExpressionOperations delegate) {
        this.delegate = delegate;
    }

    /**
     * 是否超级管理员: 主体标记 isSuperAdmin 为真, 或拥有 ROLE_sadmin 权限
     */
    private boolean isSuperAdmin() {
        Authentication authentication = delegate.getAuthentication();
        if (authentication == null) {
            return false;
        }
        if (authentication.getPrincipal() instanceof SysUser user && user.isSuperAdmin()) {
            return true;
        }
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            if (SUPER_ADMIN_AUTHORITY.equals(authority.getAuthority())) {
                return true;
            }
        }
        return false;
    }

    // ==================== 放行点(超管短路) ====================

    @Override
    public boolean hasAuthority(String authority) {
        return isSuperAdmin() || delegate.hasAuthority(authority);
    }

    @Override
    public boolean hasAnyAuthority(String... authorities) {
        return isSuperAdmin() || delegate.hasAnyAuthority(authorities);
    }

    @Override
    public boolean hasRole(String role) {
        return isSuperAdmin() || delegate.hasRole(role);
    }

    @Override
    public boolean hasAnyRole(String... roles) {
        return isSuperAdmin() || delegate.hasAnyRole(roles);
    }

    @Override
    public boolean hasPermission(Object target, Object permission) {
        return isSuperAdmin() || delegate.hasPermission(target, permission);
    }

    @Override
    public boolean hasPermission(Object targetId, String targetType, Object permission) {
        return isSuperAdmin() || delegate.hasPermission(targetId, targetType, permission);
    }

    // ==================== 其余委托默认实现 ====================

    @Override
    public Authentication getAuthentication() {
        return delegate.getAuthentication();
    }

    @Override
    public boolean permitAll() {
        return delegate.permitAll();
    }

    @Override
    public boolean denyAll() {
        return delegate.denyAll();
    }

    @Override
    public boolean isAnonymous() {
        return delegate.isAnonymous();
    }

    @Override
    public boolean isAuthenticated() {
        return delegate.isAuthenticated();
    }

    @Override
    public boolean isRememberMe() {
        return delegate.isRememberMe();
    }

    @Override
    public boolean isFullyAuthenticated() {
        return delegate.isFullyAuthenticated();
    }

    @Override
    public void setFilterObject(Object filterObject) {
        delegate.setFilterObject(filterObject);
    }

    @Override
    public Object getFilterObject() {
        return delegate.getFilterObject();
    }

    @Override
    public void setReturnObject(Object returnObject) {
        delegate.setReturnObject(returnObject);
    }

    @Override
    public Object getReturnObject() {
        return delegate.getReturnObject();
    }

    @Override
    public Object getThis() {
        return delegate.getThis();
    }
}
