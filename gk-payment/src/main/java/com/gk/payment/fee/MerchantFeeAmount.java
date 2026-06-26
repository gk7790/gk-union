package com.gk.payment.fee;

import java.math.BigDecimal;

public record MerchantFeeAmount(BigDecimal feeAmount, BigDecimal payinSettleAmount) {
}
