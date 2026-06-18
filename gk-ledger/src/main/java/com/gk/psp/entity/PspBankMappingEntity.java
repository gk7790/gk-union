package com.gk.psp.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.gk.common.core.entity.SimpleEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("psp_bank_mapping")
public class PspBankMappingEntity extends SimpleEntity {
    private Long pspId;
    private Long pspAccountId;
    private Long pspMethodId;
    private String countryCode;
    private String currency;
    private Long bankId;
    private String pspBankCode;
    private String pspBankName;
    private String pspBankShortName;
    private String direction;
    private Integer status;
    private String extra;
    private String remark;
}
