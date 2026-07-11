package com.gk.payment.domain.amount;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class MoneyFormat {
    public static String format(BigDecimal amount) {
        BigDecimal value = amount == null ? BigDecimal.ZERO : amount;
        return value.setScale(2, RoundingMode.DOWN).toPlainString();
    }
    private MoneyFormat() {}
}
