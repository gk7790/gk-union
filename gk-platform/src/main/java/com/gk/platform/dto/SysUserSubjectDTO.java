package com.gk.platform.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class SysUserSubjectDTO implements Serializable {
    private Long id;
    private Long userId;
    private String subjectType;
    private Long tenantId;
    private Long merchantId;
    private Long deptId;
    private Long roleId;
    private Integer status;
    private String remark;
    private String roleAuth;
    private String roleName;
}
