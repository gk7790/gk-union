package com.gk.payment.merchantview;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.gk.openapi.dto.OpenApiModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@OpenApiModel
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class PayinOrderView extends MerchantOrderView {
    private String payUrl;
    private String paidAmount;
    private String settleAmount;
}
