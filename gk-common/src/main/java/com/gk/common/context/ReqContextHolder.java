package com.gk.common.context;

import com.alibaba.ttl.TransmittableThreadLocal;
import org.apache.commons.collections4.CollectionUtils;

import java.util.*;

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
     * 及子集部门ID
     */
    public static Set<Long> getSubDeptIds() {
        Set<Long> deptIdList = get().getDeptIdList();
        if (CollectionUtils.isNotEmpty(deptIdList)) {
            return deptIdList;
        }
        return Set.of(-1L);
    }

    /**
     * 本部门以及子集部门ID
     */
    public static Set<Long> getSubDeptIdsWithSelf() {
        ReqContext context = get();
        Long deptId = context.getDeptId();

        Set<Long> result = getSubDeptIds();
        if (result == null) {
            result = new HashSet<>();
        }

        if (deptId != null) {
            result.add(deptId);
        }

        if (CollectionUtils.isNotEmpty(result)) {
            return result;
        }
        return Set.of(-1L);
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
    public static Integer getScope() {
        return get().getScope();
    }

    /**
     * 业务领域
     */
    public static Integer getDomain() {
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
