package com.gk.payment.plan;

import com.gk.common.redis.RedisKeys;
import org.apache.commons.lang3.StringUtils;

import java.util.Locale;

/**
 * 支付决策表运行时维度�? * <p>
 * 注意：订单金额不属于 key，金额只�?catalog 内部�?bucket 匹配�? */
public record PaymentPlanKey(
        Long tenantId,
        Long merchantId,
        Long merchantAppId,
        String direction,
        String countryCode,
        String currency,
        String methodCode
) {
    public static PaymentPlanKey of(Long tenantId,
                                    Long merchantId,
                                    Long merchantAppId,
                                    String direction,
                                    String countryCode,
                                    String currency,
                                    String methodCode) {
        return new PaymentPlanKey(
                tenantId,
                merchantId,
                merchantAppId == null ? 0L : merchantAppId,
                normalize(direction),
                normalizeNullable(countryCode),
                normalize(currency),
                normalize(methodCode)
        );
    }

    public String redisKey() {
        return RedisKeys.getPaymentPlanActiveKey(tenantId, merchantId, merchantAppId, direction, countryCode, currency, methodCode);
    }

    public String localKey() {
        return String.join(":",
                String.valueOf(tenantId),
                String.valueOf(merchantId),
                String.valueOf(merchantAppId),
                direction,
                countryCode,
                currency,
                methodCode
        );
    }

    private static String normalize(String value) {
        return StringUtils.defaultString(value).trim().toUpperCase(Locale.ROOT);
    }

    private static String normalizeNullable(String value) {
        return StringUtils.defaultString(value).trim().toUpperCase(Locale.ROOT);
    }
}
