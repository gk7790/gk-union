package com.gk.tenant.dto;

import com.gk.platform.dto.SysUserDTO;
import lombok.Data;

@Data
public class TenantOnboardRequest {
    private SysTenantDTO tenant;
    private SysUserDTO adminUser;
    private String deptName;
    private Long roleTemplateId;
}
