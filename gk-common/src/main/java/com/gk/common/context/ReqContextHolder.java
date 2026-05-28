package com.gk.common.context;

import com.alibaba.ttl.TransmittableThreadLocal;

public class ReqContextHolder {

    private ReqContextHolder() {
    }

    /**
     * 支持线程池上下文透传
     */
    private static final TransmittableThreadLocal<ReqContext>
            HOLDER =
            new TransmittableThreadLocal<>();

    /**
     * 设置上下文
     */
    public static void set(ReqContext context) {

        if (context == null) {
            return;
        }

        HOLDER.set(context);
    }

    /**
     * 获取上下文
     */
    public static ReqContext get() {

        ReqContext context = HOLDER.get();

        if (context == null) {

            context = createDefaultContext();

            HOLDER.set(context);
        }

        return context;
    }

    /**
     * 清理上下文
     */
    public static void clear() {
        HOLDER.remove();
    }

    /**
     * 用户ID
     */
    public static Long getUserId() {
        return get().getUserId();
    }

    /**
     * 租户ID
     */
    public static Long getTenantId() {
        return get().getTenantId();
    }

    /**
     * 部门ID
     */
    public static Long getDeptId() {
        return get().getDeptId();
    }

    /**
     * 当前语言
     */
    public static String getLang() {
        return get().getLang();
    }

    /**
     * 领域
     */
    public static String getScope() {
        return get().getScope();
    }

    /**
     * 业务领域
     */
    public static String getDomain() {
        return get().getDomain();
    }

    /**
     * 当前时区
     */
    public static String getTimezone() {
        return get().getTimezone();
    }

    /**
     * traceId
     */
    public static String getTraceId() {
        return get().getTraceId();
    }

    /**
     * 是否超级管理员
     */
    public static Boolean isSAdmin() {
        return Boolean.TRUE.equals(
                get().getSAdmin()
        );
    }

    /**
     * 默认上下文
     */
    private static ReqContext createDefaultContext() {
        return ReqContext.builder()
                .lang("zh-CN")
                .timezone("UTC")
                .sAdmin(false)
                .build();
    }
}
