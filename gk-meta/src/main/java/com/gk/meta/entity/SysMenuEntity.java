package com.gk.meta.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.gk.common.core.entity.SimpleEntity;
import com.gk.meta.dto.SysMenuMeta;
import lombok.Data;

/**
 * 菜单管理
 *
 * @author Lowen
 */
@Data
@TableName(value = "sys_menu", autoResultMap = true)
public class SysMenuEntity extends SimpleEntity {
    /**
     * 父菜单ID，一级菜单为0
     */
    private Long pid;
    /**
     * 租户id
     */
    private Long tenantId;
	/**
	 * 菜单名称
	 */
	private String name;
    /**
     * 地址栏路径
     */
    private String path;
    /**
     * 类型
     */
    private String type;
    /**
     * 状态
     */
    private Integer status;
    /**
     * 激活路径
     */
    private String activePath;
    /**
     * 授权(多个用逗号分隔，如：sys:user:list,sys:user:save)
     */
    private String authCode;
    /**
     * 组件路径
     */
    private String component;
    /**
     * 排序
     */
    private Integer sort;
    /**
     * 领域
     */
    private String scope;
    /**
     * 样式
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private SysMenuMeta meta;
    /**
     * 菜单标题
     */
    @TableField(exist = false)
    private String title;

}