package com.gk.payment.fee;

import com.gk.payment.entity.MerchantFeeRuleEntity;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class MerchantFeeResult {
    private MerchantFeeRuleEntity rule;
    private BigDecimal merchantFeeAmount;
    private BigDecimal settleAmount;
    private String snapshotJson;
}
