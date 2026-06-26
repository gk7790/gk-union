package com.gk.payment.enums;

import com.gk.common.annotation.EnumDict;
import com.gk.common.annotation.Style;
import com.gk.common.enums.StringCodeEnum;
import com.gk.common.enums.StyleType;
import org.apache.commons.lang3.StringUtils;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Locale;

/**
 * 代收订单结算状态 * <p>
 * PENDING：代收成功已入账至待结算账户，尚未释放到可用 * RELEASED：已释放至商户可用余额 */
@EnumDict("settleStatus")
public enum SettleStatusEnum implements StringCodeEnum {
    @Style(StyleType.WARNING)
    PENDING("PENDING", "待结", "enum.settleStatus.pending"),

    @Style(StyleType.SUCCESS)
    RELEASED("RELEASED", "已释", "enum.settleStatus.released"),

    @Style(StyleType.WARNING)
    HELD("HELD", "已冻", "enum.settleStatus.held"),

    @Style(StyleType.DANGER)
    CANCELLED("CANCELLED", "已取", "enum.settleStatus.cancelled");

    private final String code;
    private final String label;
    private final String i18nKey;

    SettleStatusEnum(String code, String label, String i18nKey) {
        this.code = code;
        this.label = label;
        this.i18nKey = i18nKey;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public String label() {
        return label;
    }

    @Override
    public String i18nKey() {
        return i18nKey;
    }

    /** AUTO 以外（含 MANUAL）均不自动释放*/
    public static boolean isAutoReleaseMode(String settleMode) {
        return StringUtils.equalsIgnoreCase(StringUtils.defaultString(settleMode).trim(), "AUTO");
    }

    /**
     * 按商户结算周期计算计划释放时刻（商户时区日历日）     * T0=支付成功即时；T1=次日起算；TN=N 个自然日0 点     */
    public static Instant computeReleaseAt(String settleCycle, Instant paidAt, String timezone) {
        Instant base = paidAt == null ? Instant.now() : paidAt;
        String cycle = StringUtils.defaultIfBlank(settleCycle, "T1").trim().toUpperCase(Locale.ROOT);
        if ("T0".equals(cycle)) {
            return base;
        }
        int days = 1;
        if (cycle.startsWith("T") && cycle.length() > 1) {
            try {
                days = Integer.parseInt(cycle.substring(1));
            } catch (NumberFormatException ignored) {
            }
        }
        if (days < 0) {
            days = 0;
        }
        ZoneId zone;
        try {
            zone = ZoneId.of(StringUtils.defaultIfBlank(timezone, "Asia/Shanghai"));
        } catch (Exception ex) {
            zone = ZoneId.of("Asia/Shanghai");
        }
        ZonedDateTime paidZoned = base.atZone(zone);
        return paidZoned.toLocalDate().plusDays(days).atStartOfDay(zone).toInstant();
    }
}
