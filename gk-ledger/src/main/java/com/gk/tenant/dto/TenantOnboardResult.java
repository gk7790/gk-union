package com.gk.tenant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(title = "租户开户结果", description = "租户开户后创建出的核心资源ID")
public class TenantOnboardResult {
    @Schema(title = "租户ID")
    private Long tenantId;

    @Schema(title = "默认部门ID")
    private Long deptId;

    @Schema(title = "租户角色ID")
    private Long roleId;

    @Schema(title = "租户管理员用户ID")
    private Long userId;
}
