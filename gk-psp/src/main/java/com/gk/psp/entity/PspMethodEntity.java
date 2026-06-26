package com.gk.psp.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.gk.common.core.entity.SimpleEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("psp_method")
public class PspMethodEntity extends SimpleEntity {
    private Long pspId;
    private String pspCode;
    private String methodCode;
    private String pspMethodCode;
    private String methodName;
    private String countryCode;
    private String currency;
    private String direction;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    private BigDecimal dailyLimit;
    private Integer status;
    private String configJson;
    private String remark;
}
