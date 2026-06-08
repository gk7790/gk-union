package com.gk.tenant.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.baomidou.mybatisplus.annotation.*;
import com.gk.common.core.entity.SimpleEntity;

/**
 * 租户信息表
 *
 * @author Lowen lowen@gmail.com
 * @since 3.0 2026-05-29
 */
@Data
@EqualsAndHashCode(callSuper=false)
@TableName("sys_tenant")
public class SysTenantEntity extends SimpleEntity {

	/**
	* 租户公司名称
	*/
	private String name;
	/**
	* 编码
	*/
	private String code;
	/**
	* 状态
	*/
	private Integer status;
	/**
	* 域名
	*/
	private String domain;
	/**
	* 货币
	*/
	private String currency;
	/**
	* 时区
	*/
	private String timezone;
	/**
	* 语言
	*/
	private String lang;
    /**
     * 备注
     */
    private String remark;
}
