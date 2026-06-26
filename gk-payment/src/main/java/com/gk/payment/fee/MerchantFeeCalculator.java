package com.gk.payment.fee;

import com.gk.common.enums.FeeBearerEnum;
import com.gk.common.enums.FeeModeEnum;
import com.gk.common.enums.StringCodeEnum;
import com.gk.common.amount.FeeLimitUtils;
import com.gk.payment.entity.MerchantFeeRuleEntity;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class MerchantFeeCalculator {
    private static final int MONEY_SCALE = 8;

    private MerchantFeeCalculator() {
    }

    public static MerchantFeeAmount calculate(BigDecimal amount, MerchantFeeRuleEntity rule) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amount must be greater than zero");
        }
        if (rule == null) {
            throw new IllegalArgumentException("Merchant fee rule is not configured");
        }

        FeeModeEnum feeMode = StringCodeEnum.fromCode(FeeModeEnum.class, rule.getFeeMode());
        if (feeMode == null) {
            throw new IllegalArgumentException("Invalid merchant fee mode");
        }
        BigDecimal fee = switch (feeMode) {
            case RATE -> amount.multiply(defaultZero(rule.getFeeRate()));
            case FIXED -> defaultZero(rule.getFeeFixed());
            case RATE_FIXED -> amount.multiply(defaultZero(rule.getFeeRate())).add(defaultZero(rule.getFeeFixed()));
        };

        fee = scale(FeeLimitUtils.apply(fee, rule.getMinFee(), rule.getMaxFee()));
        BigDecimal settleAmount = amount;
        if (!FeeBearerEnum.CUSTOMER.matches(rule.getFeeBearer())) {
            settleAmount = amount.subtract(fee);
        }
        if (settleAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Merchant fee cannot exceed order amount");
        }
        return new MerchantFeeAmount(fee, scale(settleAmount));
    }

    private static BigDecimal defaultZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static BigDecimal scale(BigDecimal value) {
        return value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }
}
