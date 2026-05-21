
package com.gk.platform.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.gk.common.core.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 岗位管理
 *
 * @author Lowen
 */
@Data
@EqualsAndHashCode(callSuper=false)
@TableName("sys_post")
public class SysPostEntity extends BaseEntity {

	/**
	* 岗位编码
	*/
	private String postCode;
	/**
	* 岗位名称
	*/
	private String postName;
	/**
	* 排序
	*/
	private Integer sort;
	/**
	* 状态
	*/
	private Integer status;
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
}