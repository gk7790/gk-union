package com.gk.common.redis;

/** Redis keys owned by framework-level capabilities. */
@SuppressWarnings("unused")
public final class RedisKeys {

    private RedisKeys() {
    }

    public static String getSysParamsKey(String key) { return "sys:params:" + key; }
    public static String getSysLonginKey(String module, String key) { return "sys:login:" + module + ":" + key; }
    public static String getDeptIdsKey(Long deptId) { return "sys:login:DeptIds:" + deptId; }
    public static String getSubjectDataScopeKey(Long subjectId) { return "sys:login:SubjectDataScope:" + subjectId; }
    public static String getSecurityUserKey(Long id) { return "sys:security:user:" + id; }
    public static String getLoginMfaChallengeKey(String token) { return "auth:mfa:challenge:" + token; }
    public static String getAuthenticatorBindKey(Long userId) { return "authenticator:bind:" + userId; }
    public static String getUserMenuNavKey(Long userId) { return "sys:user:nav:" + userId; }
    public static String getUserPermissionsKey(Long userId) { return "sys:user:permissions:" + userId; }
    public static String getLimitKey(String key) { return "sys:limit:" + key; }
    public static String getRequestKey(String key) { return "sys:request:" + key; }
    public static String getLoginIpWhitelistKey(String subjectType, Long tenantId, Long merchantId, Long subjectId) {
        return "sys:login-ip-whitelist:" + all(subjectType) + ":" + all(tenantId) + ":" + all(merchantId) + ":" + all(subjectId);
    }
    public static String getLoginIpWhitelistPattern() { return "sys:login-ip-whitelist:*"; }
    public static final long MEMBER_HOUR_EXPIRE = 60L * 60 * 24;

    private static String all(Object value) { return value == null ? "*" : String.valueOf(value); }
}
