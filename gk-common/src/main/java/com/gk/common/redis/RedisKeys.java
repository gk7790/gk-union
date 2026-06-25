package com.gk.common.redis;

import com.gk.common.tools.StringFormat;

/**
 * @author Lowen
 * @since 1.0.0
 */
@SuppressWarnings("unused")
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
     * 登录用户Key
     */
    public static String getSecurityUserKey(Long id){
        return "sys:security:user:" + id;
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

    public static String getOpenApiMerchantAppKey(String appId) {
        return "openapi:merchant-app:" + appId;
    }

    public static String getOpenApiAuthKey(String appId) {
        return "openapi:auth:app:" + appId;
    }

    public static String getSubjectDisplayKey(Long tenantId, String subjectType, Long subjectId) {
        return "ledger:subject-display:" + tenantId + ":" + subjectType + ":" + subjectId;
    }

    /**
     * 租户下拉字典缓存 Key。
     */
    public static String getTenantDictKey(String statusKey, boolean includeSystemTenant) {
        return "sys:tenant:dict:" + nullToAll(statusKey) + ":" + includeSystemTenant;
    }

    public static String getTenantDictPattern() {
        return "sys:tenant:dict:*";
    }

    /**
     * PSP 支付方式下拉字典缓存 Key。
     */
    public static String getPspMethodDictKey(String countryCode, String currency, String direction) {
        return "psp:method:dict:"
                + nullToAll(countryCode) + ":"
                + nullToAll(currency) + ":"
                + nullToAll(direction);
    }

    public static String getPspMethodDictPattern() {
        return "psp:method:dict:*";
    }

    /**
     * PSP 上游支付方式编码下拉缓存 Key。
     */
    public static String getPspMethodCodeDictKey(Long pspId) {
        return "psp:method-code:dict:" + nullToAll(pspId);
    }

    public static String getPspMethodCodeDictPattern() {
        return "psp:method-code:dict:*";
    }

    /**
     * PSP 账户下拉字典缓存 Key。
     */
    public static String getPspAccountDictKey(Long pspId) {
        return "psp:account:dict:" + nullToAll(pspId);
    }

    public static String getPspAccountDictPattern() {
        return "psp:account:dict:*";
    }

    /**
     * 系统标准支付方式下拉字典缓存 Key。
     */
    public static String getPaymentMethodDictKey(String countryCode, String currency, String direction, String methodType, String statusKey) {
        return "payment:method:dict:"
                + nullToAll(countryCode) + ":"
                + nullToAll(currency) + ":"
                + nullToAll(direction) + ":"
                + nullToAll(methodType) + ":"
                + nullToAll(statusKey);
    }

    public static String getPaymentMethodDictPattern() {
        return "payment:method:dict:*";
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

    /**
     * 当前生效的支付决策表缓存 Key。
     * <p>
     * key 只包含配置维度，不包含订单金额；金额在决策表内部按 bucket 命中。
     */
    public static String getPaymentPlanActiveKey(Long tenantId, Long merchantId, Long merchantAppId,
                                                 String direction, String countryCode, String currency, String methodCode) {
        return "payment:plan:active:" + StringFormat.join(":",
                nullToAll(tenantId),
                nullToAll(merchantId),
                nullToAll(merchantAppId),
                nullToAll(direction),
                nullToAll(countryCode),
                nullToAll(currency),
                nullToAll(methodCode)
        );
    }

    public static String getPaymentPlanActivePattern() {
        return "payment:plan:active:*";
    }

    /**
     * PSP 紧急停用 Key。
     */
    public static String getPaymentPspDisableKey(Long tenantId, String direction, Long pspId) {
        return "payment:psp:disable:"
                + nullToAll(tenantId) + ":"
                + nullToAll(direction) + ":"
                + nullToAll(pspId);
    }

    /**
     * PSP Account 紧急停用 Key。
     */
    public static String getPaymentPspAccountDisableKey(Long tenantId, String direction, Long pspAccountId) {
        return "payment:psp-account:disable:"
                + nullToAll(tenantId) + ":"
                + nullToAll(direction) + ":"
                + nullToAll(pspAccountId);
    }

    public static String getPaymentPspHealthKey(Long tenantId, String direction, Long pspId) {
        return "payment:psp:health:"
                + nullToAll(tenantId) + ":"
                + nullToAll(direction) + ":"
                + nullToAll(pspId);
    }

    public static String getPaymentPspAccountHealthKey(Long tenantId, String direction, Long pspAccountId) {
        return "payment:psp-account:health:"
                + nullToAll(tenantId) + ":"
                + nullToAll(direction) + ":"
                + nullToAll(pspAccountId);
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
