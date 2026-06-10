package com.gk.platform.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;

/**
 * 角色管理
 *
 * @author Lowen
 * @since 1.0.0
 */
@Data
@Schema(title = "角色管理")
public class SysRoleDTO implements Serializable {

	@Schema(title = "id")
	private Long id;

    @Schema(title = "租户ID")
    private Long tenantId;

    @Schema(title = "auth")
    private String auth;

	@Schema(title = "角色名称")
	private String name;

	@Schema(title = "部门名称")
	private String deptName;

    @Schema(title = "状态")
    private Integer status;

	@Schema(title = "备注")
	private String remark;

	@Schema(title = "角色适用主体范围")
	private String roleScope;

	@Schema(title = "数据权限范围")
	private String dataScope;

	@Schema(title = "创建时间")
	@JsonProperty(access = JsonProperty.Access.READ_ONLY)
	private Instant createdAt;

	@Schema(title = "菜单ID列表")
	private List<Long> menuIdList;

	@Schema(title = "部门ID列表")
	private List<Long> deptIdList;

}
