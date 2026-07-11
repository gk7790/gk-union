package com.gk.payment.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.gk.payment.dto.PaymentJsonModel;
import lombok.Data;

import java.math.BigDecimal;

@Data
@PaymentJsonModel
public class PaymentPlanBatchPreviewRequest {
    @JsonAlias("tenantId")
    private Long tenantId;
    @JsonAlias("routeGroupId")
    private Long routeGroupId;
    @JsonAlias("defaultMinAmount")
    private BigDecimal defaultMinAmount;
    @JsonAlias("defaultMaxAmount")
    private BigDecimal defaultMaxAmount;
    @JsonAlias("pspFeeRequired")
    private Boolean pspFeeRequired;
}
