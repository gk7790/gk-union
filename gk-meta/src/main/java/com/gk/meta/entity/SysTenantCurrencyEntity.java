package com.gk.meta.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.gk.common.core.entity.SimpleEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("sys_tenant_currency")
public class SysTenantCurrencyEntity extends SimpleEntity {
    private Long tenantId;
    private String currency;
    private Integer status;
    private Integer sort;
    private String remark;
}
