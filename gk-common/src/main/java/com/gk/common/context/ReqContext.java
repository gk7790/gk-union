package com.gk.common.context;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReqContext {
    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;
    /**
     * 租户ID
     */
    private Long tenantId;
    /**
     * 部门ID
     */
    private Long deptId;

    /**
     * 是否超级管理员
     */
    private Boolean sAdmin;

    /**
     * 当前语言
     */
    private String lang;

    /**
     * 当前时区
     */
    private String timezone;

    /**
     * traceId
     */
    private String traceId;

    /**
     * requestId
     */
    private String requestId;
    /**
     * 领域
     */
    private String scope;
    /**
     * 业务领域
     */
    private String domain;
}
