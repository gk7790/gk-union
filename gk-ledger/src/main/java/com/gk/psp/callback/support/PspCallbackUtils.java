package com.gk.psp.callback.support;

import com.gk.payment.enums.PayOrderStatusEnum;
import com.gk.payment.enums.PayoutOrderStatusEnum;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;

/**
 * PSP 回调处理通用工具类。
 * <p>
 * 放置状态标准化、空值兜底、金额文本化和摘要计算等无状态工具方法。
 */
public final class PspCallbackUtils {
    private PspCallbackUtils() {
    }

    /**
     * 判断订单状态是否已经进入终态。
     * <p>
     * 终态订单不允许被后续重复回调覆盖，保障回调处理幂等。
     */
    public static boolean isTerminal(String status) {
        String normalized = normalizeStatus(status);
        return PayOrderStatusEnum.SUCCESS.code().equals(normalized)
                || PayOrderStatusEnum.FAILED.code().equals(normalized)
                || PayOrderStatusEnum.CLOSED.code().equals(normalized)
                || PayoutOrderStatusEnum.CANCELLED.code().equals(normalized)
                || PayoutOrderStatusEnum.MANUAL_REVIEW.code().equals(normalized);
    }

    /**
     * 标准化 PSP 回调状态。
     */
    public static String normalizeStatus(String status) {
        return StringUtils.defaultString(status).trim().toUpperCase(Locale.ROOT);
    }

    /**
     * 金额为空时返回备用金额。
     */
    public static BigDecimal defaultAmount(BigDecimal value, BigDecimal fallback) {
        return value == null ? fallback : value;
    }

    /**
     * Long 为空时返回 0。
     */
    public static Long defaultLong(Long value) {
        return value == null ? 0L : value;
    }

    /**
     * 使用非科学计数法输出金额文本。
     */
    public static String decimalText(BigDecimal value) {
        return value == null ? null : value.toPlainString();
    }

    /**
     * 计算文本的 SHA-256 十六进制摘要。
     * <p>
     * 主要用于回调 body、商户通知 payload 的去重和完整性追踪。
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
