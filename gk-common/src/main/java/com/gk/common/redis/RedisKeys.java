package com.gk.common.redis;

/**
 * @author Lowen
 * @since 1.0.0
 */
public class RedisKeys {
    /**
     * 系统参数Key
     */
    public static String getSysParamsKey(String key){
        return "sys:params:" + key;
    }

    /**
     * 验证码Key
     */
    public static String getCaptchaKey(String uuid){
        return "sys:captcha:" + uuid;
    }

    /**
     * 验证码Key
     */
    public static String getSysLonginKey(String module, String key){
        return "sys:login:" + module + ":" + key;
    }

    /**
     * 验证码Key
     */
    public static String getDeptIdsKey(Long deptId){
        return "sys:login:DeptIds:" + deptId;
    }

    /**
     * 当前主体的数据权限部门ID缓存。
     */
    public static String getSubjectDataScopeKey(Long subjectId){
        return "sys:login:SubjectDataScope:" + subjectId;
    }

    /**
     * 验证码Key
     */
    public static String getSysLonginMerchantKey(String key){
        return "sys:login-merchant:" + key;
    }

    /**
     * 验证码Key
     */
    public static String getMerchantAccessTokenKey(String key){
        return "sys:merchant-access-token:" + key;
    }

    /**
     * 登录用户Key
     */
    public static String getSecurityUserKey(Long id){
        return "sys:security:user:" + id;
    }

    /**
     * 系统日志Key
     */
    public static String getSysLogKey(){
        return "sys:log";
    }

    /**
     * 系统资源Key
     */
    public static String getSysResourceKey(){
        return  "sys:resource";
    }

    /**
     * 用户菜单导航Key
     */
    public static String getUserMenuNavKey(Long userId){
        return "sys:user:nav:" + userId;
    }

    /**
     * 用户权限标识Key
     */
    public static String getUserPermissionsKey(Long userId){
        return "sys:user:permissions:" + userId;
    }

    /**
     * 系统参数Key
     */
    public static String getLimitKey(String key){
        return "sys:limit:" + key;
    }

    /**
     * 用户权限标识Key
     */
    public static String getRequestKey(String key){
        return "sys:request:" + key;
    }


    /**
     * 验证码Key
     */
    public static String getApiNonceKey(String module, String key){
        return "openapi:nonce:" + module + ":" + key;
    }

    /**
     * 验证码Key
     */
    public static String getApiLimitQpsKey(String module, long key){
        return "openapi:rate:" + module + ":" + key;
    }

    public static String getLoginIpWhitelistKey(String subjectType, Long tenantId, Long merchantId, Long subjectId) {
        return "sys:login-ip-whitelist:"
                + nullToAll(subjectType) + ":"
                + nullToAll(tenantId) + ":"
                + nullToAll(merchantId) + ":"
                + nullToAll(subjectId);
    }

    public static String getLoginIpWhitelistPattern() {
        return "sys:login-ip-whitelist:*";
    }

    public static String getMerchantApiIpWhitelistKey(Long tenantId, Long merchantId) {
        return "merchant:api-ip-whitelist:"
                + nullToAll(tenantId) + ":"
                + nullToAll(merchantId);
    }

    public static String getMerchantApiIpWhitelistPattern() {
        return "merchant:api-ip-whitelist:*";
    }

    public static String getTgBindTicketKey(String code) {
        return "tg:merchant:bind:" + code;
    }

    public static String getPspCallbackIpWhitelistKey(String pspCode) {
        return "psp:callback-ip-whitelist:" + nullToAll(pspCode);
    }

    public static String getPspCallbackIpWhitelistPattern() {
        return "psp:callback-ip-whitelist:*";
    }

    private static String nullToAll(Object value) {
        return value == null ? "*" : String.valueOf(value);
    }


    /**
     * redis 会员用户有效时间
     * 过期时长为24小时，单位：秒
     */
    public static long MEMBER_HOUR_EXPIRE = 60 * 60 * 24;
}
