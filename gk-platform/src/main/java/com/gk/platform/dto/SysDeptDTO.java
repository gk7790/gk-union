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
@Schema(title = "部门管理", description = "平台或租户内部组织部门，用于用户归属和部门数据权限")
public class SysDeptDTO extends TreeNode implements Serializable {

    @Schema(title = "租户ID", description = "租户部门所属租户；平台部门可为空")
    private Long tenantId;

	@Schema(title = "部门ID", accessMode = Schema.AccessMode.READ_ONLY)
	private Long id;

	@Schema(title = "上级部门ID", description = "根部门为 0")
	private Long pid;

	@Schema(title = "部门名称", requiredMode = Schema.RequiredMode.REQUIRED)
	private String name;

	@Schema(title = "排序")
	private Integer sort;

    @Schema(title = "状态", description = "1正常 2暂停 3停用")
    private Integer status;

	@Schema(title = "创建时间")
	@JsonProperty(access = JsonProperty.Access.READ_ONLY)
	private LocalDateTime createdAt;

	@Schema(title = "上级部门名称", accessMode = Schema.AccessMode.READ_ONLY)
	private String parentName;

	@Schema(title = "备注")
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
