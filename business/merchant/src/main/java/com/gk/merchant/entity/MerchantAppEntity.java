package com.gk.merchant.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.gk.common.core.entity.SimpleEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.Instant;

/**
 * 商户API接入应用
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("merchant_app")
public class MerchantAppEntity extends SimpleEntity {
    private Long tenantId;
    private Long merchantId;
    private String appId;
    private String appName;
    private String appType;
    private String appEnv;
    private Integer status;
    private String signType;
    private String encryptType;
    private String apiSecret;
    private Integer secretVersion;
    private Instant secretUpdatedAt;
    private String notifyUrl;
    private String returnUrl;
    private Integer rateLimitQps;
    private Integer nonceTtlSeconds;
    private String remark;
}
