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
public class PayoutOrderView extends MerchantOrderView {
    @JSONField(name = "debit_amount")
    private String debitAmount;
}
