package com.gk.platform.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.gk.common.core.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户主体与角色关系。
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("sys_role_user")
public class SysRoleUserEntity extends BaseEntity {
    private Long userSubjectId;
    private Long userId;
    private Long roleId;
}
