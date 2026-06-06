package com.gk.common.context;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReqContext {

    /**
     * 模块
     */
    private String model;
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
     * 子集部门id
     */
    private Set<Long> deptIdList;

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
    private Integer scope;
    /**
     * 业务领域
     */
    private Integer domain;

    /**
     * IP
     */
    private String ip;

    /**
     * 国家
     */
    private String country;

    /**
     * 城市
     */
    private String city;

    /**
     * UserAgent
     */
    private String userAgent;

    /**
     * 设备
     */
    private String device;

    /**
     * 浏览器
     */
    private String browser;

    /**
     * 操作系统
     */
    private String os;

    /**
     * URI
     */
    private String uri;

    /**
     * Method
     */
    private String method;

    /**
     * Header
     */
    private Map<String, String> headers;
}
