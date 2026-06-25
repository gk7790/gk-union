package com.gk.tenant.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.gk.common.core.entity.SimpleEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 租户信息�?
 *
 * @author Lowen lowen@gmail.com
 * @since 3.0 2026-05-29
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("sys_tenant")
public class TenantEntity extends SimpleEntity {

    private String name;
    private String code;
    private Integer status;
    private String domain;
    private String currency;
    private String timezone;
    private String lang;
    private String remark;
}
