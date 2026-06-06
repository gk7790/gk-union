package com.gk.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.gk.common.model.DynMap;

import java.io.Serial;
import java.util.List;
import java.util.Map;
import java.util.Set;


@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthUser extends DynMap {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 当前登入用户ID
     */
    public Long getId() { return getLong("id"); }
    public void setId(Long userId) {put("id", userId);}

    /**
     * 当前登入用户部门ID
     */
    public Long getDeptId() { return getLong("deptId"); }
    public void setDeptId(Long deptId) {put("deptId", deptId);}

    /**
     * 当前登入用户部门deptIdList
     */
    public List<Long> getDeptIdList() { return getList("deptIdList", Long.class); }
    public void setDeptIdList(Set<Long> deptIdList) {put("deptIdList", deptIdList);}


    /**
     * 当前登入用户租户ID
     */
    public Long getTenantId() { return getLong("tenantId"); }
    public void setTenantId(Long tenantId) {put("tenantId", tenantId);}

    /**
     * 当前登入用户登入标识
     */
    public String getUName() { return getStr("uname"); }
    public void setUName(String uname) {put("uname", uname);}

    /**
     * 当前登入用户名称
     */
    public String getName() { return getStr("name"); }
    public void setName(String name) {put("name", name);}

    /**
     * 当前登入用户昵称
     */
    public String getNickName() { return getStr("nickName"); }
    public void setNickName(String nickName) {put("nickName", nickName);}

    /**
     * 当前登入用户真实名称
     */
    public String getRealName() { return getStr("realName"); }
    public void setRealName(String realName) {put("realName", realName);}

    /**
     * 当前登入用户邮箱
     */
    public String getEmail() { return getStr("email"); }
    public void setEmail(String email) {put("email", email);}

    /**
     * 当前登入用户是否是超级管理员
     */
    public int getSAdmin() {
        return getInt("sAdmin", 0);
    }
    public void setSAdmin(Integer sadmin) {put("sAdmin", sadmin);}
    public boolean isSAdmin() { return getSAdmin() == 1; }

    public List<String> getRoleList() { return getList("roleList", String.class);}
    public void setRoleList(Set<String> roleAuthList) {put("roleList", roleAuthList);}

    public List<String> getAuthList() { return getList("authList", String.class);}
    public void setAuthList(Set<String> roleAuthList) {put("authList", roleAuthList);}

    /**
     * 当前登入用户授权
     */
    public List<String> getAuthCode() { return getList("authCode", String.class);}
    public void setAuthCode(List<String> authCode) {put("authCode", authCode);}


    /**
     * 字段映射关系：
     * Java 对象字段名 -> AuthUser/DataMap 中保存的 key
     */
    private static final Map<String, String> FIELD_KEY_MAPPING = Map.of(
            "id", "id",
            "userId", "id",
            "uName", "uname",
            "username", "uname",
            "sAdmin", "sadmin",
            "superAdmin", "sadmin"
    );

    /**
     * 将任意对象转成 AuthUser
     */
    public static AuthUser fromObject(Map<String, Object> source) {
        AuthUser authUser = new AuthUser();

        if (source == null) {
            return authUser;
        }

        // 如果是 Map，直接处理 Map
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            if (entry.getKey() == null) {
                continue;
            }

            Object value = entry.getValue();

            if (value == null) {
                continue;
            }

            authUser.put(entry.getKey(), value);
        }
        return authUser;
    }
}
