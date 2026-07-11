package com.gk.common.redis;

import com.gk.common.tools.StringFormat;

/** Redis keys owned by the payment, merchant, PSP and OpenAPI domains. */
public final class PaymentRedisKeys {
    private PaymentRedisKeys() {}

    public static String getApiNonceKey(String module, String key) { return "openapi:nonce:" + module + ":" + key; }
    public static String getApiLimitQpsKey(String module, long key) { return "openapi:rate:" + module + ":" + key; }
    public static String getOpenApiMerchantAppKey(String appId) { return "openapi:merchant-app:" + appId; }
    public static String getOpenApiAuthKey(String appId) { return "openapi:auth:app:" + appId; }
    public static String getSubjectDisplayKey(Long tenantId, String subjectType, Long subjectId) { return "ledger:subject-display:" + tenantId + ":" + subjectType + ":" + subjectId; }
    public static String getTenantDictKey(String statusKey, boolean includeSystemTenant) { return "sys:tenant:dict:" + all(statusKey) + ":" + includeSystemTenant; }
    public static String getTenantDictPattern() { return "sys:tenant:dict:*"; }
    public static String getPspMethodCodeDictKey(Long pspId) { return "psp:method-code:dict:" + all(pspId); }
    public static String getPspMethodCodeDictPattern() { return "psp:method-code:dict:*"; }
    public static String getPspAccountDictKey(Long pspId) { return "psp:account:dict:" + all(pspId); }
    public static String getPspAccountDictPattern() { return "psp:account:dict:*"; }
    public static String getPaymentMethodDictKey(String countryCode, String currency, String direction, String methodType, String statusKey) { return "payment:method:dict:" + all(countryCode) + ":" + all(currency) + ":" + all(direction) + ":" + all(methodType) + ":" + all(statusKey); }
    public static String getPaymentMethodDictPattern() { return "payment:method:dict:*"; }
    public static String getDeeplinkTestTokenKey(String token) { return "tools:deeplink-test:" + token; }
    public static String getPspCallbackIpWhitelistKey(String pspCode) { return "psp:callback-ip-whitelist:" + all(pspCode); }
    public static String getPspCallbackIpWhitelistPattern() { return "psp:callback-ip-whitelist:*"; }
    public static String getPspCallbackAccountKey(String accountNo) { return "psp:callback-account:" + all(accountNo); }
    public static String getPspCallbackAccountPattern() { return "psp:callback-account:*"; }
    public static String getPaymentPlanActiveKey(Long tenantId, Long merchantId, Long appId, String direction, String country, String currency, String method) { return "payment:plan:active:" + StringFormat.join(":", all(tenantId), all(merchantId), all(appId), all(direction), all(country), all(currency), all(method)); }
    public static String getPaymentPlanActivePattern() { return "payment:plan:active:*"; }
    public static String getPaymentPspDisableKey(Long tenantId, String direction, Long pspId) { return "payment:psp:disable:" + all(tenantId) + ":" + all(direction) + ":" + all(pspId); }
    public static String getPaymentPspAccountDisableKey(Long tenantId, String direction, Long accountId) { return "payment:psp-account:disable:" + all(tenantId) + ":" + all(direction) + ":" + all(accountId); }
    public static String getPaymentPspHealthKey(Long tenantId, String direction, Long pspId) { return "payment:psp:health:" + all(tenantId) + ":" + all(direction) + ":" + all(pspId); }
    public static String getPaymentPspAccountHealthKey(Long tenantId, String direction, Long accountId) { return "payment:psp-account:health:" + all(tenantId) + ":" + all(direction) + ":" + all(accountId); }
    public static String getPaymentPspBalanceKey(Long tenantId, Long accountId) { return "payment:psp-account:balance:" + all(tenantId) + ":" + all(accountId); }
    public static String getPaymentPspBalancePattern() { return "payment:psp-account:balance:*"; }

    private static String all(Object value) { return value == null ? "*" : String.valueOf(value); }
}
