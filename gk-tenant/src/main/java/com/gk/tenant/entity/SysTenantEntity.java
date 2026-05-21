package com.gk.tenant.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.gk.common.core.entity.SimpleEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_tenant")
public class SysTenantEntity extends SimpleEntity {
    private String name;
    private String code;
    private Integer status;
    private Integer planType;
}
