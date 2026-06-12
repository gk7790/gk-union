package com.gk.psp.callback.support;

import com.gk.payment.enums.PayOrderStatusEnum;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;

public final class PspCallbackUtils {
    private PspCallbackUtils() {
    }

    public static boolean isTerminal(String status) {
        String normalized = normalizeStatus(status);
        return PayOrderStatusEnum.SUCCESS.code().equals(normalized)
                || PayOrderStatusEnum.FAILED.code().equals(normalized);
    }

    public static String normalizeStatus(String status) {
        return StringUtils.defaultString(status).trim().toUpperCase(Locale.ROOT);
    }

    public static BigDecimal defaultAmount(BigDecimal value, BigDecimal fallback) {
        return value == null ? fallback : value;
    }

    public static Long defaultLong(Long value) {
        return value == null ? 0L : value;
    }

    public static String decimalText(BigDecimal value) {
        return value == null ? null : value.toPlainString();
    }

    public static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(StringUtils.defaultString(value).getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte item : hash) {
                builder.append(String.format("%02x", item));
            }
            return builder.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }
}
