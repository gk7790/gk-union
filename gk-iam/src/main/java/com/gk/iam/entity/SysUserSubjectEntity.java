package com.gk.iam.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.gk.common.core.entity.SimpleEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("sys_user_subject")
public class SysUserSubjectEntity extends SimpleEntity {
    private Long userId;
    private String subjectType;
    private Long tenantId;
    private Long merchantId;
    private Long deptId;
    private Integer status;
    private String remark;

    @TableField(exist = false)
    private String roleAuth;

    @TableField(exist = false)
    private String roleName;
}
