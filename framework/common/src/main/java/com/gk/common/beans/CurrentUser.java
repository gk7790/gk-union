package com.gk.common.beans;

import com.gk.common.dto.AuthUser;

public interface CurrentUser {

    /**
     * 获取当前用户ID
     */
    Long getUserId();

    /**
     * 获取当前用户名
     */
    String getUsername();

    /**
     * 获取当前部门ID
     */
    Long getDeptId();

    /**
     * 获取当前租户ID
     */
    Long getTenantId();

    /**
     * 是否是管理员
     */
    boolean isAdmin();

    /**
     * 登入用户信息
     */
    AuthUser getAuthUser();

    /**
     * 检查是否有权限
     */
    boolean hasAllAuth(String... auths);
    boolean hasAnyAuth(String... auths);

    /**
     * 检查是否有角色
     */
    boolean hasAnyRole(String... roles);
    boolean hasAllRole(String... roles);
}
