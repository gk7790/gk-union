package com.gk.openapi.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PayoutOrderQueryRequest {
    @JsonAlias({"payout_order_no", "system_order_id"})
    private String payoutOrderNo;
    @JsonAlias("merchant_order_id")
    private String merchantOrderNo;
}
