package com.gk.auth.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gk.common.constant.Constant;
import com.gk.common.dto.AuthUser;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.Set;

@Data
public class SysUser implements UserDetails {

    private Long  id;
    private Long subjectId;
    private Long tenantId;
    private Long merchantId;
    private Long deptId;
    private Long roleId;
    private List<Long> roleIdList;
    private String subjectType;
    private String roleAuth;
    private String username;
    private String nickName;
    private String password;
    private String email;
    private String avatar;
    private Integer status;
    private Integer authType;
    private String authSecret;
    private String realName;
    private Integer gender;

    /**
     * 模块: admin(管理后台用户), zap(内网穿透用户), relay(节点客户端用户)
     */
    private String sub;
    /**
     *部门数据权限
     */
    private Set<String> roleList;
    /**
     *部门数据权限
     */
    private Set<String> authList;
    /**
     * 部门数据权限
     */
    @JsonIgnore
    private Set<Long> deptIdList;
    /**
     * 权限标识
     */
    @JsonIgnore
    private List<SimpleGrantedAuthority> authorities;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    public boolean isSuperAdmin() {
        if (isSuperAdminAuth(roleAuth)) {
            return true;
        }
        return roleList != null && roleList.stream().anyMatch(this::isSuperAdminAuth);
    }

    private boolean isSuperAdminAuth(String auth) {
        return auth != null
                && (Constant.ROLE_AUTH_SADMIN.equalsIgnoreCase(auth)
                || "SUPER_ADMIN".equalsIgnoreCase(auth));
    }

    public AuthUser toAuthUser() {
        AuthUser authUser = new AuthUser();
        authUser.setId(this.id);
        authUser.setSubjectId(this.subjectId);
        authUser.setTenantId(this.tenantId);
        authUser.setMerchantId(this.merchantId);
        authUser.setDeptId(this.deptId);
        authUser.setRoleId(this.roleId);
        authUser.setRoleIdList(this.roleIdList);
        authUser.setSubjectType(this.subjectType);
        authUser.setUName(this.username);
        authUser.setNickName(nickName);
        authUser.setEmail(email);
        authUser.setSAdmin(this.isSuperAdmin());
        authUser.setDeptIdList(deptIdList);
        authUser.setRoleList(roleList);
        authUser.setAuthList(authList);
        return authUser;
    }

    @Override
    public boolean isEnabled() {
        return status == null || status == 1;
    }
}
