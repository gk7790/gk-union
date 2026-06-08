package com.gk.payment.fee;

import com.gk.payment.entity.MerchantFeeRuleEntity;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

public final class MerchantFeeCalculator {
    private static final int MONEY_SCALE = 8;
    private static final String FEE_MODE_RATE = "RATE";
    private static final String FEE_MODE_FIXED = "FIXED";
    private static final String FEE_MODE_RATE_FIXED = "RATE_FIXED";
    private static final String FEE_BEARER_CUSTOMER = "CUSTOMER";

    private MerchantFeeCalculator() {
    }

    public static MerchantFeeAmount calculate(BigDecimal amount, MerchantFeeRuleEntity rule) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amount must be greater than zero");
        }
        if (rule == null) {
            throw new IllegalArgumentException("Merchant fee rule is not configured");
        }

        BigDecimal fee = switch (normalize(rule.getFeeMode())) {
            case FEE_MODE_RATE -> amount.multiply(defaultZero(rule.getFeeRate()));
            case FEE_MODE_FIXED -> defaultZero(rule.getFeeFixed());
            case FEE_MODE_RATE_FIXED -> amount.multiply(defaultZero(rule.getFeeRate())).add(defaultZero(rule.getFeeFixed()));
            default -> throw new IllegalArgumentException("Invalid merchant fee mode");
        };

        if (rule.getMinFee() != null && fee.compareTo(rule.getMinFee()) < 0) {
            fee = rule.getMinFee();
        }
        if (rule.getMaxFee() != null && fee.compareTo(rule.getMaxFee()) > 0) {
            fee = rule.getMaxFee();
        }

        fee = scale(fee);
        BigDecimal settleAmount = amount;
        if (!FEE_BEARER_CUSTOMER.equals(normalize(rule.getFeeBearer()))) {
            settleAmount = amount.subtract(fee);
        }
        if (settleAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Merchant fee cannot exceed order amount");
        }
        return new MerchantFeeAmount(fee, scale(settleAmount));
    }

    private static String normalize(String value) {
        return StringUtils.defaultString(value).trim().toUpperCase(Locale.ROOT);
    }

    private static BigDecimal defaultZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static BigDecimal scale(BigDecimal value) {
        return value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }
}
