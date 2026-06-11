package com.gk.openapi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@OpenApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class PayOrderQueryRequest {
    /** 平台订单号 */
    private String systemOrderId;
    /** 商户订单号 */
    private String merchantOrderId;
}
