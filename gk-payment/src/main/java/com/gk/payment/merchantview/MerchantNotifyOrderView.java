package com.gk.payment.merchantview;

import com.alibaba.fastjson2.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.gk.payment.dto.PaymentJsonModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@PaymentJsonModel
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class MerchantNotifyOrderView extends MerchantOrderView {
    @JSONField(name = "merchant_id")
    private String merchantId;
    @JSONField(name = "app_id")
    private String appId;
    @JSONField(name = "direction")
    private String direction;
    @JSONField(name = "pay_url")
    private String payUrl;
    @JSONField(name = "paid_amount")
    private String paidAmount;
    @JSONField(name = "settle_amount")
    private String settleAmount;
    @JSONField(name = "debit_amount")
    private String debitAmount;
}
