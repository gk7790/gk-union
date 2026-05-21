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
     * redis 会员用户有效时间
     * 过期时长为24小时，单位：秒
     */
    public static long MEMBER_HOUR_EXPIRE = 60 * 60 * 24;

    /**
     * 白名单列表Key
     */
    public static final String SYS_WHITE_LIST_KEY = "sys:whitelist";
}
