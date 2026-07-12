package com.gk.reference.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.gk.common.core.entity.SimpleEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("sys_bank")
public class SysBankEntity extends SimpleEntity {
    private String countryCode;
    private String currency;
    private String bankCode;
    private String bankName;
    private String bankShortName;
    private String swiftCode;
    private String localClearingCode;
    private Integer status;
    private Integer sort;
    private String remark;
}
