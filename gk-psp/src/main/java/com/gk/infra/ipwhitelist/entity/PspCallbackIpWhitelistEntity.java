package com.gk.infra.ipwhitelist.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.gk.common.core.entity.SimpleEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("psp_callback_ip_whitelist")
public class PspCallbackIpWhitelistEntity extends SimpleEntity {
    private String pspCode;
    private String ruleName;
    private String ipPattern;
    private Integer status;
    private String remark;
}
