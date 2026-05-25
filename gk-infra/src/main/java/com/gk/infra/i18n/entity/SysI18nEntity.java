package com.gk.infra.i18n.entity;

import com.gk.common.core.entity.SimpleEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.baomidou.mybatisplus.annotation.*;

/**
 * 系统-国际化
 *
 * @author Lowen lowen@gmail.com
 * @since 3.0 2026-05-23
 */
@Data
@EqualsAndHashCode(callSuper=false)
@TableName("sys_i18n")
public class SysI18nEntity extends SimpleEntity {

	/**
	* NULL=全局；有值=租户覆盖
	*/
	private Long tenantId;
	/**
	* MENU / DICT / GAME / CONFIG / NOTICE / PAGE
	*/
	private String bizType;
	/**
	* 对应业务ID
	*/
	private Long bizId;
	/**
	* 国际化key
	*/
	private String i18nKey;
	/**
	* zh_CN / en_US
	*/
	private String lang;
	/**
	* 翻译内容
	*/
	private String value;
}