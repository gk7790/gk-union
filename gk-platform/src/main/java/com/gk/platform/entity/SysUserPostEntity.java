
package com.gk.platform.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.gk.common.core.entity.BaseEntity;
import lombok.Data;

/**
 * 用户岗位关系
 *
 * @author Lowen
 */
@Data
@TableName("sys_user_post")
public class SysUserPostEntity extends BaseEntity {
	/**
	* 岗位ID
	*/
	private Long postId;
	/**
	* 用户ID
	*/
	private Long userId;
}