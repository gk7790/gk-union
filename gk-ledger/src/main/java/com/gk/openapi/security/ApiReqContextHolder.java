package com.gk.openapi.security;

public class ApiReqContextHolder {
    private static final ThreadLocal<ApiReqContext> HOLDER = new ThreadLocal<>();

    private ApiReqContextHolder() {
    }

    public static void set(ApiReqContext context) {
        HOLDER.set(context);
    }

    public static ApiReqContext get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }

    public static Long getTenantId() {
        ApiReqContext context = get();
        return context == null ? null : context.getTenantId();
    }

    public static Long getMerchantId() {
        ApiReqContext context = get();
        return context == null ? null : context.getMerchantId();
    }

    public static Long getMerchantAppId() {
        ApiReqContext context = get();
        return context == null ? null : context.getMerchantAppId();
    }

    public static String getAppId() {
        ApiReqContext context = get();
        return context == null ? null : context.getAppId();
    }

}
