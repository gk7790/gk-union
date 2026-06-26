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
    private String countryCode;
    private String currency;
    private String bankCode;
    private String pspBankCode;
    private Integer status;
    private Integer sort;
    private String remark;
}
