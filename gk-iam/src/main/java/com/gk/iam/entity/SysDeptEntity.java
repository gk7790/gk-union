package com.gk.iam.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.gk.common.core.entity.SimpleEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 部门管理
 * 
 * @author Lowen
 */
@Data
@EqualsAndHashCode(callSuper=false)
@TableName("sys_dept")
public class SysDeptEntity extends SimpleEntity {

    /**
     * 租户id
     */
    private Long tenantId;
	/**
	 * 上级ID
	 */
	private Long pid;
	/**
	 * 所有上级ID，用逗号分开
	 */
	private String pids;
	/**
	 * 部门名称
	 */
	private String name;
    /**
     * 状态
     */
    private Integer status;
	/**
	 * 排序
	 */
	private Integer sort;
    /**
     * 备注
     */
    private String remark;
	/**
	 * 上级部门名称
	 */
	@TableField(exist = false)
	private String parentName;

}