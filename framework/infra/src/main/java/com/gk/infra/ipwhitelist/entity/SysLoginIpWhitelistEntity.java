package com.gk.infra.ipwhitelist.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.gk.common.core.entity.SimpleEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("sys_login_ip_whitelist")
public class SysLoginIpWhitelistEntity extends SimpleEntity {
    private String subjectType;
    private Long tenantId;
    private Long merchantId;
    private Long subjectId;
    private String ruleName;
    private String ipPattern;
    private Integer status;
    private String remark;
}
