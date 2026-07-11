package com.gk.payment.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.gk.payment.dto.PaymentJsonModel;
import lombok.Data;

import java.math.BigDecimal;

@Data
@PaymentJsonModel
public class PaymentPlanPublishRequest {
    @JsonAlias("tenantId")
    private Long tenantId;
    @JsonAlias("merchantId")
    private Long merchantId;
    @JsonAlias("merchantAppId")
    private Long merchantAppId;
    @JsonAlias("merchantFeeRuleId")
    private Long merchantFeeRuleId;
    private String direction;
    private String currency;
    @JsonAlias("countryCode")
    private String countryCode;
    @JsonAlias("methodCode")
    private String methodCode;
    @JsonAlias("minAmount")
    private BigDecimal minAmount;
    @JsonAlias("maxAmount")
    private BigDecimal maxAmount;
    @JsonAlias("pspFeeRequired")
    private Boolean pspFeeRequired;
    private String remark;
}
