package com.gk.infra.log.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import com.baomidou.mybatisplus.annotation.*;

import java.time.Instant;
import java.time.LocalDateTime;

/**
 * 系统异常日志
 *
 * @author Lowen lowen@gmail.com
 * @since 3.0 2026-05-29
 */
@Data
@EqualsAndHashCode(callSuper=false)
@TableName("sys_log_error")
public class LogErrorEntity {

	/**
	* 主键
	*/
	@TableId
	private Long id;
	/**
	* 链路追踪ID
	*/
	private String traceId;
	/**
	* 租户ID
	*/
	private Long tenantId;
	/**
	* 用户ID
	*/
	private Long userId;
	/**
	* 用户名
	*/
	private String username;
	/**
	* 模块
	*/
	private String module;
	/**
	* 服务名
	*/
	private String serviceName;
	/**
	* 异常类型
	*/
	private String errorType;
	/**
	* 业务错误码
	*/
	private String errorCode;
	/**
	* 异常消息
	*/
	private String errorMessage;
	/**
	* 完整堆栈
	*/
	private String stackTrace;
	/**
	* 堆栈HASH(用于聚合)
	*/
	private String stackHash;
	/**
	* 请求地址
	*/
	private String requestUri;
	/**
	* 请求方式
	*/
	private String requestMethod;
	/**
	* 请求参数
	*/
	private String requestParams;
	/**
	* 请求Body
	*/
	private String requestBody;
	/**
	* 请求头
	*/
	private String requestHeaders;
	/**
	* IP地址
	*/
	private String ip;
	/**
	* 国家
	*/
	private String country;
	/**
	* 省份
	*/
	private String province;
	/**
	* 城市
	*/
	private String city;
	/**
	* UA
	*/
	private String userAgent;
	/**
	* 设备类型
	*/
	private String deviceType;
	/**
	* 操作系统
	*/
	private String os;
	/**
	* 浏览器
	*/
	private String browser;
	/**
	* HTTP状态码
	*/
	private Integer httpStatus;
	/**
	* 日志级别
	*/
	private String level;
	/**
	* 是否已处理
	*/
	private Integer resolved;
	/**
	* 是否已告警
	*/
	private Integer alarmed;
	/**
	* 运行环境
	*/
	private String env;
	/**
	* 创建时间
	*/
	private Instant createdAt;
}