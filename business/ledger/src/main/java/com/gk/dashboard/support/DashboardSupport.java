package com.gk.dashboard.support;

import com.gk.payment.domain.enums.PayDirectionEnum;
import com.gk.dashboard.dto.TenantDashboardSummaryDTO;
import com.gk.dashboard.dto.TenantDashboardTrendDTO;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class DashboardSupport {

    public static final int MONEY_SCALE = 8;
    public static final int RATE_SCALE = 2;
    public static final int TODO_LIMIT_MAX = 50;
    public static final int RECENT_ORDER_LIMIT_MAX = 20;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

    private DashboardSupport() {
    }

    public record RangeWindow(String range, Instant start, Instant end, Instant compareStart, Instant compareEnd) {
    }

    public static RangeWindow resolveRange(String range, String timezone, boolean compare) {
        String normalizedRange = normalizeRange(range);
        ZoneId zoneId = resolveZoneId(timezone);
        LocalDate today = LocalDate.now(zoneId);

        LocalDate startDate;
        LocalDate endDate;
        LocalDate compareStartDate;
        LocalDate compareEndDate;

        switch (normalizedRange) {
            case "yesterday" -> {
                startDate = today.minusDays(1);
                endDate = today;
                compareStartDate = today.minusDays(2);
                compareEndDate = today.minusDays(1);
            }
            case "last7d" -> {
                startDate = today.minusDays(6);
                endDate = today.plusDays(1);
                compareStartDate = today.minusDays(13);
                compareEndDate = today.minusDays(6);
            }
            case "last30d" -> {
                startDate = today.minusDays(29);
                endDate = today.plusDays(1);
                compareStartDate = today.minusDays(59);
                compareEndDate = today.minusDays(29);
            }
            default -> {
                startDate = today;
                endDate = today.plusDays(1);
                compareStartDate = today.minusDays(1);
                compareEndDate = today;
            }
        }

        Instant compareStart = compare ? startOfDay(compareStartDate, zoneId) : null;
        Instant compareEnd = compare ? startOfDay(compareEndDate, zoneId) : null;
        return new RangeWindow(
                normalizedRange,
                startOfDay(startDate, zoneId),
                startOfDay(endDate, zoneId),
                compareStart,
                compareEnd
        );
    }

    public static String normalizeRange(String range) {
        String value = StringUtils.defaultIfBlank(range, "today").trim().toLowerCase(Locale.ROOT);
        return switch (value) {
            case "yesterday", "last7d", "last30d" -> value;
            default -> "today";
        };
    }

    public static String normalizeTrendRange(String range) {
        String value = StringUtils.defaultIfBlank(range, "last7d").trim().toLowerCase(Locale.ROOT);
        return switch (value) {
            case "today", "yesterday", "last7d", "last30d" -> value;
            default -> "last7d";
        };
    }

    public static ZoneId resolveZoneId(String timezone) {
        try {
            return ZoneId.of(StringUtils.defaultIfBlank(timezone, "UTC"));
        } catch (Exception ex) {
            return ZoneId.of("UTC");
        }
    }

    public static String resolveTzOffset(ZoneId zoneId, LocalDate date) {
        String offset = date.atStartOfDay(zoneId).getOffset().getId();
        return "Z".equals(offset) ? "+00:00" : offset;
    }

    public static List<TenantDashboardTrendDTO.TrendPoint> mergeTrendPoints(RangeWindow window,
                                                                            ZoneId zoneId,
                                                                            List<TenantDashboardTrendDTO.TrendPoint> payTrend,
                                                                            List<TenantDashboardTrendDTO.TrendPoint> payoutTrend) {
        Map<String, TenantDashboardTrendDTO.TrendPoint> pointMap = new LinkedHashMap<>();
        LocalDate startDate = LocalDate.ofInstant(window.start, zoneId);
        LocalDate endDate = LocalDate.ofInstant(window.end, zoneId);
        for (LocalDate date = startDate; date.isBefore(endDate); date = date.plusDays(1)) {
            TenantDashboardTrendDTO.TrendPoint point = new TenantDashboardTrendDTO.TrendPoint();
            point.setDate(date.format(DATE_FORMAT));
            point.setPayInAmount(BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP));
            point.setPayOutAmount(BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP));
            point.setPayInCount(0L);
            point.setPayOutCount(0L);
            pointMap.put(point.getDate(), point);
        }

        if (payTrend != null) {
            for (TenantDashboardTrendDTO.TrendPoint row : payTrend) {
                if (row == null || StringUtils.isBlank(row.getDate())) {
                    continue;
                }
                TenantDashboardTrendDTO.TrendPoint point = pointMap.get(row.getDate());
                if (point == null) {
                    continue;
                }
                point.setPayInCount(defaultLong(row.getPayInCount()));
                point.setPayInAmount(money(row.getPayInAmount()));
            }
        }

        if (payoutTrend != null) {
            for (TenantDashboardTrendDTO.TrendPoint row : payoutTrend) {
                if (row == null || StringUtils.isBlank(row.getDate())) {
                    continue;
                }
                TenantDashboardTrendDTO.TrendPoint point = pointMap.get(row.getDate());
                if (point == null) {
                    continue;
                }
                point.setPayOutCount(defaultLong(row.getPayOutCount()));
                point.setPayOutAmount(money(row.getPayOutAmount()));
            }
        }

        return new ArrayList<>(pointMap.values());
    }

    public static TenantDashboardSummaryDTO.OrderMetric normalizeOrderMetric(TenantDashboardSummaryDTO.OrderMetric metric,
                                                                             boolean includeClosed) {
        TenantDashboardSummaryDTO.OrderMetric result = metric == null ? new TenantDashboardSummaryDTO.OrderMetric() : metric;
        result.setSuccessCount(defaultLong(result.getSuccessCount()));
        result.setSuccessAmount(money(result.getSuccessAmount()));
        result.setFeeAmount(money(result.getFeeAmount()));
        result.setFailedCount(defaultLong(result.getFailedCount()));
        result.setProcessingCount(defaultLong(result.getProcessingCount()));
        result.setSuccessRate(successRate(
                result.getSuccessCount(),
                result.getFailedCount(),
                includeClosed ? defaultLong(result.getClosedCount()) : 0L
        ));
        return result;
    }

    public static TenantDashboardSummaryDTO.CompareMetric compareMetric(TenantDashboardSummaryDTO.OrderMetric current,
                                                                        TenantDashboardSummaryDTO.OrderMetric previous) {
        TenantDashboardSummaryDTO.CompareMetric compare = new TenantDashboardSummaryDTO.CompareMetric();
        compare.setSuccessCountDelta(current.getSuccessCount() - previous.getSuccessCount());
        compare.setSuccessAmountDelta(current.getSuccessAmount().subtract(previous.getSuccessAmount()));
        if (current.getSuccessRate() != null && previous.getSuccessRate() != null) {
            compare.setSuccessRateDelta(current.getSuccessRate().subtract(previous.getSuccessRate()));
        }
        return compare;
    }

    public static TenantDashboardSummaryDTO.Balance normalizeBalance(TenantDashboardSummaryDTO.Balance row) {
        TenantDashboardSummaryDTO.Balance balance = row == null ? new TenantDashboardSummaryDTO.Balance() : row;
        balance.setAvailable(money(balance.getAvailable()));
        balance.setFrozen(money(balance.getFrozen()));
        balance.setPendingSettle(money(balance.getPendingSettle()));
        balance.setTotal(money(balance.getTotal()));
        balance.setMerchantCount(defaultLong(balance.getMerchantCount()));
        return balance;
    }

    public static TenantDashboardSummaryDTO.Todo normalizeTodo(TenantDashboardSummaryDTO.Todo row) {
        TenantDashboardSummaryDTO.Todo todo = row == null ? new TenantDashboardSummaryDTO.Todo() : row;
        todo.setManualReview(defaultLong(todo.getManualReview()));
        todo.setNotifyFailed(defaultLong(todo.getNotifyFailed()));
        todo.setSettleDue(defaultLong(todo.getSettleDue()));
        todo.setProcessingPay(defaultLong(todo.getProcessingPay()));
        todo.setProcessingPayout(defaultLong(todo.getProcessingPayout()));
        return todo;
    }

    public static BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    public static long defaultLong(Long value) {
        return value == null ? 0L : value;
    }

    public static String parseBizType(String bizType) {
        if (StringUtils.isBlank(bizType)) {
            return null;
        }
        return switch (bizType.trim().toUpperCase(Locale.ROOT)) {
            case "PAYIN" -> PayDirectionEnum.PAYIN.code();
            case "PAYOUT" -> PayDirectionEnum.PAYOUT.code();
            default -> null;
        };
    }

    private static BigDecimal successRate(Long successCount, Long failedCount, Long closedCount) {
        long denominator = defaultLong(successCount) + defaultLong(failedCount) + defaultLong(closedCount);
        if (denominator <= 0) {
            return null;
        }
        return BigDecimal.valueOf(defaultLong(successCount))
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), RATE_SCALE, RoundingMode.HALF_UP);
    }

    private static Instant startOfDay(LocalDate date, ZoneId zoneId) {
        return date.atStartOfDay(zoneId).toInstant();
    }
}
