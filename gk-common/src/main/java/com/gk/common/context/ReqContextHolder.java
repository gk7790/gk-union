package com.gk.common.context;

import com.alibaba.ttl.TransmittableThreadLocal;
import com.gk.common.enums.SubjectTypeEnum;
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
     * 用户主体ID
     */
    public static Long getSubjectId() {
        return get().getSubjectId();
    }

    /**
     * 租户ID
     */
    public static Long getTenantId() {
        return get().getTenantId();
    }

    /**
     * 商户ID
     */
    public static Long getMerchantId() {
        return get().getMerchantId();
    }

    /**
     * 当前主体角色ID
     */
    public static Long getRoleId() {
        return get().getRoleId();
    }

    /**
     * 当前主体角色ID列表
     */
    public static List<Long> getRoleIdList() {
        List<Long> roleIdList = get().getRoleIdList();
        if (roleIdList != null && !roleIdList.isEmpty()) {
            return roleIdList;
        }
        Long roleId = getRoleId();
        return roleId == null ? List.of() : List.of(roleId);
    }

    /**
     * 主体类型
     */
    public static String getSubjectType() {
        return get().getSubjectType();
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
    public static Boolean isSuperAdmin() {
        return Boolean.TRUE.equals(
                get().getSAdmin()
        );
    }

    /**
     * 判断是否是平台级用户
     */
    public static boolean isPlatform() {
        return isSuperAdmin() || SubjectTypeEnum.PLATFORM.matches(ReqContextHolder.getSubjectType());
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
