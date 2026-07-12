package com.gk.psp.fee;

import com.gk.payment.domain.amount.FeeLimitUtils;
import com.gk.payment.domain.enums.FeeModeEnum;
import com.gk.common.enums.StringCodeEnum;
import com.gk.psp.entity.PspFeeRuleEntity;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class PspFeeCalculator {
    private static final int MONEY_SCALE = 8;

    private PspFeeCalculator() {
    }

    public static BigDecimal calculate(BigDecimal amount, PspFeeRuleEntity rule) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amount must be greater than zero");
        }
        if (rule == null) {
            throw new IllegalArgumentException("PSP fee rule is not configured");
        }

        FeeModeEnum feeMode = StringCodeEnum.fromCode(FeeModeEnum.class, rule.getFeeMode());
        if (feeMode == null) {
            throw new IllegalArgumentException("Invalid PSP fee mode");
        }
        BigDecimal fee = switch (feeMode) {
            case RATE -> amount.multiply(defaultZero(rule.getFeeRate()));
            case FIXED -> defaultZero(rule.getFeeFixed());
            case RATE_FIXED -> amount.multiply(defaultZero(rule.getFeeRate())).add(defaultZero(rule.getFeeFixed()));
        };

        return FeeLimitUtils.apply(fee, rule.getMinFee(), rule.getMaxFee()).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private static BigDecimal defaultZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
