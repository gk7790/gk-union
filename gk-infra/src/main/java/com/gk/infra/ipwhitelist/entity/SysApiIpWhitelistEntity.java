package com.gk.infra.ipwhitelist.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.gk.common.core.entity.SimpleEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("sys_api_ip_whitelist")
public class SysApiIpWhitelistEntity extends SimpleEntity {
    private String apiType;
    private Long tenantId;
    private Long merchantId;
    private String ruleName;
    private String ipPattern;
    private Integer status;
    private String remark;
}
