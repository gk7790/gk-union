package com.gk.iam.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.gk.common.core.entity.SimpleEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper=false)
@TableName("sys_user")
public class SysUserEntity extends SimpleEntity {

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

    @TableField(exist=false)
    private Long subjectId;

    @TableField(exist=false)
    private String subjectType;

    @TableField(exist=false)
    private Long tenantId;

    @TableField(exist=false)
    private Long merchantId;

    @TableField(exist=false)
    private Long deptId;

    @TableField(exist=false)
    private Long roleId;

    @TableField(exist=false)
    private List<Long> roleIdList;

}
