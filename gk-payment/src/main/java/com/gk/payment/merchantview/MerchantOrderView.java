package com.gk.payment.merchantview;

import com.alibaba.fastjson2.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.gk.payment.dto.PaymentJsonModel;
import lombok.Data;

@Data
@PaymentJsonModel
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class MerchantOrderView {
    @JSONField(name = "system_order_id")
    private String systemOrderId;
    @JSONField(name = "merchant_order_id")
    private String merchantOrderId;
    @JSONField(name = "status")
    private String status;
    @JSONField(name = "status_reason")
    private String statusReason;
    @JSONField(name = "amount")
    private String amount;
    @JSONField(name = "currency")
    private String currency;
    @JSONField(name = "country_code")
    private String countryCode;
    @JSONField(name = "method_code")
    private String methodCode;
    @JSONField(name = "fee_amount")
    private String feeAmount;
}
