package com.gk.psp.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.gk.common.core.entity.SimpleEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("psp_merchant")
public class PspMerchantEntity extends SimpleEntity {
    private Long tenantId;
    private Long merchantId;
    private Long merchantScopeId;
    private Long pspId;
    private String pspCode;
    private String pspMerchantNo;
    private String pspMerchantName;
    private Integer status;
    private String secretType;
    private String apiKey;
    private String apiSecret;
    private String merchantPrivateKeyRef;
    private String pspPublicKey;
    private String callbackSecret;
    private String configJson;
    private String remark;
}
