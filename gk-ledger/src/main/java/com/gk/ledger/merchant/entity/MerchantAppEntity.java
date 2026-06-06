package com.gk.ledger.merchant.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.gk.common.core.entity.SimpleEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("merchant_app")
public class MerchantAppEntity extends SimpleEntity {
    private Long tenantId;
    private Long merchantId;
    private String appId;
    private String appName;
    private String apiSecret;
    private String notifyUrl;
    private String ipWhitelist;
    private Integer status;
    private String remark;
}
