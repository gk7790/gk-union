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
@Schema(title = "角色管理", description = "角色决定菜单权限、按钮权限和数据范围；主体归属由 sys_user_subject 管理")
public class SysRoleDTO implements Serializable {

	@Schema(title = "角色ID", accessMode = Schema.AccessMode.READ_ONLY)
	private Long id;

    @Schema(title = "租户ID", description = "0=系统模板，1=平台专属，≥2=租户实例")
    private Long tenantId;

    @Schema(title = "角色编码", description = "权限标识；sadmin/admin 仅超管可设置")
    private String auth;

	@Schema(title = "角色名称")
	private String name;

	@Schema(title = "部门名称")
	private String deptName;

    @Schema(title = "状态", description = "1正常 2暂停 3停用")
    private Integer status;

	@Schema(title = "备注")
	private String remark;

	@Schema(title = "角色适用主体范围", description = "PLATFORM 平台角色，TENANT 租户角色，MERCHANT 商户角色")
	private String roleScope;

	@Schema(title = "数据权限范围", description = "ALL/TENANT_ALL/SELF_AND_CHILDREN/SELF")
	private String dataScope;

	@Schema(title = "是否只读", description = "系统预置角色为 true，非超管不可修改", accessMode = Schema.AccessMode.READ_ONLY)
	private Boolean readOnly;

	@Schema(title = "创建时间")
	@JsonProperty(access = JsonProperty.Access.READ_ONLY)
	private Instant createdAt;

	@Schema(title = "菜单ID列表")
	private List<Long> menuIdList;

	@Schema(title = "部门ID列表")
	private List<Long> deptIdList;

}
