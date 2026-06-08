package com.gk.openapi.security;

public class OpenApiRequestContextHolder {
    private static final ThreadLocal<OpenApiRequestContext> HOLDER = new ThreadLocal<>();

    private OpenApiRequestContextHolder() {
    }

    public static void set(OpenApiRequestContext context) {
        HOLDER.set(context);
    }

    public static OpenApiRequestContext get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }

    public static Long getTenantId() {
        OpenApiRequestContext context = get();
        return context == null ? null : context.getTenantId();
    }

    public static Long getMerchantId() {
        OpenApiRequestContext context = get();
        return context == null ? null : context.getMerchantId();
    }

    public static Long getMerchantAppId() {
        OpenApiRequestContext context = get();
        return context == null ? null : context.getMerchantAppId();
    }

    public static String getAppId() {
        OpenApiRequestContext context = get();
        return context == null ? null : context.getAppId();
    }

    public static String getRequestId() {
        OpenApiRequestContext context = get();
        return context == null ? null : context.getRequestId();
    }
}
