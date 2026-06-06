package com.gk.ledger.merchant.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.gk.common.core.entity.SimpleEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("merchant")
public class MerchantEntity extends SimpleEntity {
    private Long tenantId;
    private String merchantNo;
    private String merchantName;
    private Integer status;
    private String countryCode;
    private String currency;
    private String remark;
}
