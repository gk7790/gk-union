package com.gk.tenant.dto;

import com.gk.iam.dto.SysUserDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(title = "租户开户请", description = "平台创建租户时同步创建默认部门、租户管理员账号和租户角色绑")
public class TenantOnboardRequest {
    @Schema(title = "租户资料", description = "租户名称、编码、币种、时区、语言等基础信息", requiredMode = Schema.RequiredMode.REQUIRED)
    private TenantDTO tenant;

    @Schema(title = "租户管理员账", description = "开户时创建的租户管理员登录账号", requiredMode = Schema.RequiredMode.REQUIRED)
    private SysUserDTO adminUser;

    @Schema(title = "默认部门名称", description = "为空时默认创建“默认部门")
    private String deptName;

    @Schema(title = "租户角色模板ID", description = "平台预置TENANT 角色模板ID，开户时会复制为该租户自己的角色", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long roleTemplateId;
}
