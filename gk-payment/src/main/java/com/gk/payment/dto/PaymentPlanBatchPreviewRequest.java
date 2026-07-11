package com.gk.payment.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.gk.openapi.dto.OpenApiModel;
import lombok.Data;

import java.math.BigDecimal;

@Data
@OpenApiModel
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
