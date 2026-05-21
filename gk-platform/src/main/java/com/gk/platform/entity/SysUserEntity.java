package com.gk.platform.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.gk.common.core.entity.SimpleEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper=false)
@TableName("sys_user")
public class SysUserEntity extends SimpleEntity {

    /**
     * 部门ID
     */
    private Long deptId;
    /**
     * 租户id
     */
    private Long tenantId;
    /**
     * 用户名
     */
    private String username;
    /**
     * 昵称
     */
    private String nickname;
    /**
     * 密码
     */
    private String password;
    /**
     * 姓名
     */
    private String realName;
    /**
     * 头像
     */
    private String avatar;
    /**
     * 性别   0：男   1：女    2：保密
     */
    private Integer gender;
    /**
     * 邮箱
     */
    private String email;
    /**
     * 手机号
     */
    private String mobile;
    /**
     * 超级管理员   0：否   1：是
     */
    private Integer superAdmin;
    /**
     * 状态  0：停用   1：正常
     */
    private Integer status;
    /**
     * 验证器类型
     */
    private Integer authType;
    /**
     * 验证器秘钥
     */
    private String authSecret;
    /**
     * 部门名称
     */
    @TableField(exist=false)
    private String deptName;
    /**
     * 租户名称
     */
    @TableField(exist=false)
    private String tenantName;

}
