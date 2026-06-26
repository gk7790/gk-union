package com.gk.psp.callback.support;

import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;

/**
 * Shared utility methods for PSP callback processing.
 * Covers status normalization, null fallbacks, amount text formatting and digest calculation.
 */
public final class PspCallbackUtils {
    public static final String STATUS_PROCESSING = "PROCESSING";
    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_CANCELLED = "CANCELLED";
    public static final String STATUS_CLOSED = "CLOSED";
    public static final String STATUS_MANUAL_REVIEW = "MANUAL_REVIEW";

    private PspCallbackUtils() {
    }

    /**
     * Returns whether the status has reached a terminal state, including manual review.
     */
    public static boolean isTerminal(String status) {
        String normalized = normalizeStatus(status);
        return isFinalTerminal(normalized)
                || PspCallbackUtils.STATUS_MANUAL_REVIEW.equals(normalized);
    }

    /**
     * Returns whether the status is a final immutable terminal state.
     */
    public static boolean isFinalTerminal(String status) {
        String normalized = normalizeStatus(status);
        return PspCallbackUtils.STATUS_SUCCESS.equals(normalized)
                || PspCallbackUtils.STATUS_FAILED.equals(normalized)
                || PspCallbackUtils.STATUS_CLOSED.equals(normalized)
                || PspCallbackUtils.STATUS_CANCELLED.equals(normalized);
    }

    /**
     * Normalizes PSP callback status text.
     */
    public static String normalizeStatus(String status) {
        return StringUtils.defaultString(status).trim().toUpperCase(Locale.ROOT);
    }

    /**
     * Returns the fallback amount when the value is null.
     */
    public static BigDecimal defaultAmount(BigDecimal value, BigDecimal fallback) {
        return value == null ? fallback : value;
    }

    /**
     * Returns zero when the value is null.
     */
    public static Long defaultLong(Long value) {
        return value == null ? 0L : value;
    }

    /**
     * Formats amount text without scientific notation.
     */
    public static String decimalText(BigDecimal value) {
        return value == null ? null : value.toPlainString();
    }

    /**
     * Calculates the SHA-256 hex digest of text.
     */
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
