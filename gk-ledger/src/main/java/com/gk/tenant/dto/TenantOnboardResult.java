package com.gk.tenant.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TenantOnboardResult {
    private Long tenantId;
    private Long deptId;
    private Long roleId;
    private Long userId;
}
