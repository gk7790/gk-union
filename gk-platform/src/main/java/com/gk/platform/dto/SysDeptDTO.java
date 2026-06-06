package com.gk.platform.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.gk.common.model.TreeNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 部门管理
 *
 * @author Lowen
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(title = "部门管理")
public class SysDeptDTO extends TreeNode implements Serializable {

    @Schema(title = "租户ID")
    private Long tenantId;

	@Schema(title = "id")
	private Long id;

	@Schema(title = "上级ID")
	private Long pid;

	@Schema(title = "部门名称")
	private String name;

	@Schema(title = "排序")
	private Integer sort;

    @Schema(title = "状态")
    private Integer status;

	@Schema(title = "创建时间")
	@JsonProperty(access = JsonProperty.Access.READ_ONLY)
	private LocalDateTime createdAt;

	@Schema(title = "上级部门名称")
	private String parentName;

    private String remark;

	@Override
	public Long getId() {
		return id;
	}

	@Override
	public void setId(Long id) {
		this.id = id;
	}

	@Override
	public Long getPid() {
		return pid;
	}

	@Override
	public void setPid(Long pid) {
		this.pid = pid;
	}
}
