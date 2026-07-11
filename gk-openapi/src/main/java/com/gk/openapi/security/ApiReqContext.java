package com.gk.openapi.security;

import com.gk.merchant.entity.MerchantAppEntity;
import com.gk.merchant.entity.MerchantEntity;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ApiReqContext {
    private Long tenantId;
    private Long merchantId;
    private String merchantNo;
    private Long merchantAppId;
    private String appId;
    private String traceId;
    private String clientIp;
    private MerchantEntity merchant;
    private MerchantAppEntity merchantApp;
}
