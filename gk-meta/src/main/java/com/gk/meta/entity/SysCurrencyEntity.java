package com.gk.meta.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.gk.common.core.entity.SimpleEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("sys_currency")
public class SysCurrencyEntity extends SimpleEntity {
    private String currency;
    private String currencyName;
    private String currencySymbol;
    private String numericCode;
    private Integer minorUnit;
    private Integer status;
    private Integer sort;
    private String remark;
}
