package com.gk.platform.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户管理
 *
 * @author Lowen
 * @since 1.0.0
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(title = "系统用户", description = "统一登录账号及其主体绑定信息")
public class SysUserDTO implements Serializable {

	@Schema(title = "用户ID", accessMode = Schema.AccessMode.READ_ONLY)
	private Long id;

	@Schema(title = "用户主体ID", accessMode = Schema.AccessMode.READ_ONLY)
	private Long subjectId;

	@Schema(title = "租户ID", description = "TENANT/MERCHANT 主体必填；平台主体为空")
    private Long tenantId;

	@Schema(title = "商户ID", description = "MERCHANT 主体必填；平台/租户主体为空")
    private Long merchantId;

	@Schema(title = "部门ID", description = "租户内部部门ID，用于部门数据权限")
    private Long deptId;

	@Schema(title = "主体类型", description = "PLATFORM 平台主体，TENANT 租户主体，MERCHANT 商户主体", example = "TENANT")
    private String subjectType;

	@Schema(title = "角色ID", description = "用户当前主体绑定的角色ID")
    private Long roleId;

	@Schema(title = "用户名", requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;

	@Schema(title = "昵称")
    private String nickname;

	@Schema(title = "登录密码", accessMode = Schema.AccessMode.WRITE_ONLY)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
	private String password;

	@Schema(title = "真实姓名")
    private String realName;

	@Schema(title = "头像")
    private String avatar;

	@Schema(title = "性别", description = "0男 1女 2保密")
    private Integer gender;

	@Schema(title = "邮箱")
    private String email;

	@Schema(title = "手机号")
    private String mobile;

	@Schema(title = "状态", description = "1正常 2暂停 3停用")
	private Integer status;

	@Schema(title = "创建时间", accessMode = Schema.AccessMode.READ_ONLY)
	@JsonProperty(access = JsonProperty.Access.READ_ONLY)
	private LocalDateTime createdAt;

	@Schema(title = "兼容角色ID列表", description = "当前单主体模型只取第一个角色ID")
	private List<Long> roleIdList;

	@Schema(title = "部门名称", accessMode = Schema.AccessMode.READ_ONLY)
	private String deptName;

	@Schema(title = "租户名称", accessMode = Schema.AccessMode.READ_ONLY)
    private String tenantName;

	@Schema(title = "验证器类型")
	private Integer authType;

	@Schema(title = "验证器密钥")
	private String authSecret;

}
