package com.gk.iam.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.gk.common.core.entity.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 角色
 * 
 * @author Lowen
 */
@Data
@EqualsAndHashCode(callSuper=false)
@TableName("sys_role")
public class SysRoleEntity extends BaseEntity {
    /**
     * 租户ID
     */
    private Long tenantId;
    /**
     * 角色名称
     */
    private String auth;
	/**
	 * 角色名称
	 */
	private String name;
	/**
	 * 备注
	 */
	private String remark;
	/**
	 * 角色适用主体范围：PLATFORM/TENANT/MERCHANT
	 */
	private String roleScope;
	/**
	 * 数据权限范围
	 */
	private String dataScope;
    /**
     * 状态
     */
    private Integer status;
	/**
	 * 部门ID
	 */
	@TableField(fill = FieldFill.INSERT)
	private Long deptId;
	/**
	 * 更新者
	 */
	@TableField(fill = FieldFill.INSERT_UPDATE)
	private Long updatedBy;
	/**
	 * 更新时间
	 */
	@TableField(fill = FieldFill.INSERT_UPDATE)
	private LocalDateTime updatedAt;

	@TableField(exist = false)
	private String deptName;
}