package com.gk.merchant.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.gk.common.core.entity.SimpleEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 支付商户主体
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("merchant")
public class MerchantEntity extends SimpleEntity {
    private Long tenantId;
    private String merchantNo;
    private String merchantName;
    private String merchantShortName;
    private String merchantType;
    private Integer status;
    private String riskStatus;
    private String countryCode;
    private String defaultCurrency;
    private String timezone;
    private String lang;
    private String contactName;
    private String contactEmail;
    private String contactPhone;
    private String businessLicenseNo;
    private String settleMode;
    private String settleCycle;
    private BigDecimal minSettleAmount;
    private String configJson;
    private String remark;
}
