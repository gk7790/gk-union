package com.gk.dashboard.service;

import com.gk.common.context.ReqContextHolder;
import com.gk.common.enums.SubjectTypeEnum;
import com.gk.common.exception.ErrorCode;
import com.gk.common.exception.GkException;
import com.gk.dashboard.dao.TenantDashboardDao;
import com.gk.dashboard.dto.TenantDashboardRecentOrderDTO;
import com.gk.dashboard.dto.TenantDashboardSummaryDTO;
import com.gk.dashboard.dto.TenantDashboardTodoDTO;
import com.gk.dashboard.dto.TenantDashboardTopMerchantDTO;
import com.gk.dashboard.dto.TenantDashboardTrendDTO;
import com.gk.tenant.dto.TenantDTO;
import com.gk.tenant.service.TenantService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

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

@Service
@RequiredArgsConstructor
public class TenantDashboardService {

    private static final int MONEY_SCALE = 8;
    private static final int RATE_SCALE = 2;
    private static final int TODO_LIMIT_MAX = 50;
    private static final int TOP_MERCHANT_LIMIT_MAX = 20;
    private static final int RECENT_ORDER_LIMIT_MAX = 20;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

    private final TenantDashboardDao tenantDashboardDao;
    private final TenantService tenantService;

    public TenantDashboardSummaryDTO summary(String range, String currency, boolean compare) {
        assertTenantScope();
        Long tenantId = ReqContextHolder.getTenantId();

        TenantDTO tenant = tenantService.get(tenantId);
        if (tenant == null) {
            throw new GkException(ErrorCode.NOT_FOUND);
        }

        String resolvedCurrency = resolveCurrency(currency, tenant.getCurrency());
        RangeWindow window = resolveRange(range, tenant.getTimezone(), compare);

        TenantDashboardSummaryDTO.OrderMetric payIn = normalizeOrderMetric(
                tenantDashboardDao.selectPayinOrderStats(tenantId, resolvedCurrency, window.start, window.end),
                true);
        TenantDashboardSummaryDTO.OrderMetric payOut = normalizeOrderMetric(
                tenantDashboardDao.selectPayoutOrderStats(tenantId, resolvedCurrency, window.start, window.end),
                false);

        TenantDashboardSummaryDTO summary = new TenantDashboardSummaryDTO();
        summary.setMeta(buildMeta(tenant, resolvedCurrency, window));
        summary.setPayIn(payIn);
        summary.setPayOut(payOut);
        if (compare) {
            TenantDashboardSummaryDTO.OrderMetric payInPrev = normalizeOrderMetric(
                    tenantDashboardDao.selectPayinOrderStats(tenantId, resolvedCurrency, window.compareStart, window.compareEnd),
                    true);
            TenantDashboardSummaryDTO.OrderMetric payOutPrev = normalizeOrderMetric(
                    tenantDashboardDao.selectPayoutOrderStats(tenantId, resolvedCurrency, window.compareStart, window.compareEnd),
                    false);
            summary.setPayInCompare(compareMetric(payIn, payInPrev));
            summary.setPayOutCompare(compareMetric(payOut, payOutPrev));
        }
        summary.setActiveMerchantCount(defaultLong(tenantDashboardDao.countActiveMerchants(
                tenantId, resolvedCurrency, window.start, window.end)));
        summary.setMerchantCount(normalizeMerchantCount(tenantDashboardDao.selectMerchantCount(tenantId)));
        summary.setBalance(normalizeBalance(tenantDashboardDao.selectBalanceSummary(tenantId, resolvedCurrency)));
        summary.setTodos(normalizeTodo(tenantDashboardDao.selectTodoCounts(tenantId, resolvedCurrency)));
        return summary;
    }

    public TenantDashboardTrendDTO trend(String range, String currency) {
        assertTenantScope();
        Long tenantId = ReqContextHolder.getTenantId();

        TenantDTO tenant = tenantService.get(tenantId);
        if (tenant == null) {
            throw new GkException(ErrorCode.NOT_FOUND);
        }

        String resolvedCurrency = resolveCurrency(currency, tenant.getCurrency());
        String timezone = StringUtils.defaultIfBlank(tenant.getTimezone(), "UTC");
        ZoneId zoneId = resolveZoneId(timezone);
        RangeWindow window = resolveRange(normalizeTrendRange(range), timezone, false);
        String tzOffset = resolveTzOffset(zoneId, LocalDate.ofInstant(window.start, zoneId));

        List<TenantDashboardTrendDTO.TrendPoint> payTrend = tenantDashboardDao.selectPayTrend(
                tenantId, resolvedCurrency, window.start, window.end, tzOffset);
        List<TenantDashboardTrendDTO.TrendPoint> payoutTrend = tenantDashboardDao.selectPayoutTrend(
                tenantId, resolvedCurrency, window.start, window.end, tzOffset);

        TenantDashboardTrendDTO trend = new TenantDashboardTrendDTO();
        trend.setCurrency(resolvedCurrency);
        trend.setRange(window.range);
        trend.setTimezone(timezone);
        trend.setPoints(mergeTrendPoints(window, zoneId, payTrend, payoutTrend));
        return trend;
    }

    public TenantDashboardTodoDTO todos(String type, String currency, int limit) {
        assertTenantScope();
        String todoType = parseTodoType(type);
        if (todoType == null) {
            throw new GkException(ErrorCode.BAD_REQUEST);
        }

        Long tenantId = ReqContextHolder.getTenantId();
        TenantDTO tenant = tenantService.get(tenantId);
        if (tenant == null) {
            throw new GkException(ErrorCode.NOT_FOUND);
        }

        String resolvedCurrency = resolveCurrency(currency, tenant.getCurrency());
        int resolvedLimit = Math.min(Math.max(limit, 1), TODO_LIMIT_MAX);
        List<TenantDashboardTodoDTO.TodoItem> items = tenantDashboardDao.listTodos(
                tenantId, resolvedCurrency, todoType, resolvedLimit);
        if (items == null) {
            items = List.of();
        } else {
            items.forEach(this::normalizeTodoItem);
        }

        TenantDashboardTodoDTO result = new TenantDashboardTodoDTO();
        result.setType(todoType);
        result.setCurrency(resolvedCurrency);
        result.setItems(items);
        return result;
    }

    public TenantDashboardTopMerchantDTO topMerchants(String range, String currency, int limit) {
        assertTenantScope();
        Long tenantId = ReqContextHolder.getTenantId();

        TenantDTO tenant = tenantService.get(tenantId);
        if (tenant == null) {
            throw new GkException(ErrorCode.NOT_FOUND);
        }

        String resolvedCurrency = resolveCurrency(currency, tenant.getCurrency());
        RangeWindow window = resolveRange(normalizeRange(range), tenant.getTimezone(), false);
        int resolvedLimit = Math.min(Math.max(limit, 1), TOP_MERCHANT_LIMIT_MAX);

        List<TenantDashboardTopMerchantDTO.TopMerchant> items = tenantDashboardDao.selectTopMerchants(
                tenantId, resolvedCurrency, window.start, window.end, resolvedLimit);
        if (items == null) {
            items = List.of();
        } else {
            items.forEach(this::normalizeTopMerchant);
        }

        TenantDashboardTopMerchantDTO result = new TenantDashboardTopMerchantDTO();
        result.setCurrency(resolvedCurrency);
        result.setRange(window.range);
        result.setItems(items);
        return result;
    }

    public TenantDashboardRecentOrderDTO recentOrders(String bizType, String currency, int limit) {
        assertTenantScope();
        String resolvedBizType = parseBizType(bizType);
        if (resolvedBizType == null) {
            throw new GkException(ErrorCode.BAD_REQUEST);
        }

        Long tenantId = ReqContextHolder.getTenantId();
        TenantDTO tenant = tenantService.get(tenantId);
        if (tenant == null) {
            throw new GkException(ErrorCode.NOT_FOUND);
        }

        String resolvedCurrency = resolveCurrency(currency, tenant.getCurrency());
        int resolvedLimit = Math.min(Math.max(limit, 1), RECENT_ORDER_LIMIT_MAX);
        List<TenantDashboardRecentOrderDTO.RecentOrder> items = "PAY".equals(resolvedBizType)
                ? tenantDashboardDao.selectRecentPayinOrders(tenantId, resolvedCurrency, resolvedLimit)
                : tenantDashboardDao.selectRecentPayoutOrders(tenantId, resolvedCurrency, resolvedLimit);
        if (items == null) {
            items = List.of();
        } else {
            items.forEach(item -> normalizeRecentOrder(item, resolvedBizType));
        }

        TenantDashboardRecentOrderDTO result = new TenantDashboardRecentOrderDTO();
        result.setBizType(resolvedBizType);
        result.setCurrency(resolvedCurrency);
        result.setItems(items);
        return result;
    }

    private String parseBizType(String bizType) {
        if (StringUtils.isBlank(bizType)) {
            return null;
        }
        return switch (bizType.trim().toUpperCase(Locale.ROOT)) {
            case "PAY" -> "PAY";
            case "PAYOUT" -> "PAYOUT";
            default -> null;
        };
    }

    private void normalizeRecentOrder(TenantDashboardRecentOrderDTO.RecentOrder item, String bizType) {
        item.setAmount(money(item.getAmount()));
        if ("PAY".equals(bizType)) {
            item.setRoutePath("/payment/pay-order/detail?id=" + item.getOrderId());
        } else {
            item.setRoutePath("/payment/payout-order/detail?id=" + item.getOrderId());
        }
    }

    private void normalizeTopMerchant(TenantDashboardTopMerchantDTO.TopMerchant item) {
        item.setPayInAmount(money(item.getPayInAmount()));
        item.setPayOutAmount(money(item.getPayOutAmount()));
        item.setTotalAmount(money(item.getTotalAmount()));
        item.setPayInCount(defaultLong(item.getPayInCount()));
        item.setPayOutCount(defaultLong(item.getPayOutCount()));
    }

    private String parseTodoType(String type) {
        if (StringUtils.isBlank(type)) {
            return null;
        }
        return switch (type.trim()) {
            case "MANUAL_REVIEW", "manualReview" -> "MANUAL_REVIEW";
            case "NOTIFY_FAILED", "notifyFailed" -> "NOTIFY_FAILED";
            case "SETTLE_DUE", "settleDue" -> "SETTLE_DUE";
            case "PROCESSING_PAY", "processingPay" -> "PROCESSING_PAY";
            case "PROCESSING_PAYOUT", "processingPayout" -> "PROCESSING_PAYOUT";
            default -> null;
        };
    }

    private void normalizeTodoItem(TenantDashboardTodoDTO.TodoItem item) {
        item.setAmount(money(item.getAmount()));
        if ("PAY".equalsIgnoreCase(item.getBizType())) {
            item.setRoutePath("/payment/pay-order/detail?id=" + item.getOrderId());
        } else if ("PAYOUT".equalsIgnoreCase(item.getBizType())) {
            item.setRoutePath("/payment/payout-order/detail?id=" + item.getOrderId());
        }
    }

    private String normalizeTrendRange(String range) {
        String value = StringUtils.defaultIfBlank(range, "last7d").trim().toLowerCase(Locale.ROOT);
        return switch (value) {
            case "today", "yesterday", "last7d", "last30d" -> value;
            default -> "last7d";
        };
    }

    private List<TenantDashboardTrendDTO.TrendPoint> mergeTrendPoints(RangeWindow window,
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

    private String resolveTzOffset(ZoneId zoneId, LocalDate date) {
        return date.atStartOfDay(zoneId).getOffset().getId();
    }

    private void assertTenantScope() {
        if (!SubjectTypeEnum.TENANT.matches(ReqContextHolder.getSubjectType())) {
            throw new GkException(ErrorCode.FORBIDDEN);
        }
        if (ReqContextHolder.getTenantId() == null) {
            throw new GkException(ErrorCode.DATA_SCOPE_PARAMS_ERROR);
        }
    }

    private String resolveCurrency(String requestCurrency, String tenantCurrency) {
        String value = StringUtils.defaultIfBlank(requestCurrency, tenantCurrency);
        if (StringUtils.isBlank(value)) {
            throw new GkException(ErrorCode.BAD_REQUEST);
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private TenantDashboardSummaryDTO.Meta buildMeta(TenantDTO tenant, String currency, RangeWindow window) {
        TenantDashboardSummaryDTO.Meta meta = new TenantDashboardSummaryDTO.Meta();
        meta.setTenantId(tenant.getId());
        meta.setTenantName(tenant.getName());
        meta.setTimezone(StringUtils.defaultIfBlank(tenant.getTimezone(), "UTC"));
        meta.setCurrency(currency);
        meta.setRange(window.range);
        meta.setRangeStart(window.start);
        meta.setRangeEnd(window.end);
        meta.setCompareRangeStart(window.compareStart);
        meta.setCompareRangeEnd(window.compareEnd);
        return meta;
    }

    private TenantDashboardSummaryDTO.OrderMetric normalizeOrderMetric(TenantDashboardSummaryDTO.OrderMetric metric,
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

    private TenantDashboardSummaryDTO.CompareMetric compareMetric(TenantDashboardSummaryDTO.OrderMetric current,
                                                                  TenantDashboardSummaryDTO.OrderMetric previous) {
        TenantDashboardSummaryDTO.CompareMetric compare = new TenantDashboardSummaryDTO.CompareMetric();
        compare.setSuccessCountDelta(current.getSuccessCount() - previous.getSuccessCount());
        compare.setSuccessAmountDelta(current.getSuccessAmount().subtract(previous.getSuccessAmount()));
        if (current.getSuccessRate() != null && previous.getSuccessRate() != null) {
            compare.setSuccessRateDelta(current.getSuccessRate().subtract(previous.getSuccessRate()));
        }
        return compare;
    }

    private TenantDashboardSummaryDTO.MerchantCount normalizeMerchantCount(TenantDashboardSummaryDTO.MerchantCount row) {
        TenantDashboardSummaryDTO.MerchantCount count = row == null ? new TenantDashboardSummaryDTO.MerchantCount() : row;
        count.setTotal(defaultLong(count.getTotal()));
        count.setEnabled(defaultLong(count.getEnabled()));
        return count;
    }

    private TenantDashboardSummaryDTO.Balance normalizeBalance(TenantDashboardSummaryDTO.Balance row) {
        TenantDashboardSummaryDTO.Balance balance = row == null ? new TenantDashboardSummaryDTO.Balance() : row;
        balance.setAvailable(money(balance.getAvailable()));
        balance.setFrozen(money(balance.getFrozen()));
        balance.setPendingSettle(money(balance.getPendingSettle()));
        balance.setTotal(money(balance.getTotal()));
        balance.setMerchantCount(defaultLong(balance.getMerchantCount()));
        return balance;
    }

    private TenantDashboardSummaryDTO.Todo normalizeTodo(TenantDashboardSummaryDTO.Todo row) {
        TenantDashboardSummaryDTO.Todo todo = row == null ? new TenantDashboardSummaryDTO.Todo() : row;
        todo.setManualReview(defaultLong(todo.getManualReview()));
        todo.setNotifyFailed(defaultLong(todo.getNotifyFailed()));
        todo.setSettleDue(defaultLong(todo.getSettleDue()));
        todo.setProcessingPay(defaultLong(todo.getProcessingPay()));
        todo.setProcessingPayout(defaultLong(todo.getProcessingPayout()));
        return todo;
    }

    private RangeWindow resolveRange(String range, String timezone, boolean compare) {
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

    private String normalizeRange(String range) {
        String value = StringUtils.defaultIfBlank(range, "today").trim().toLowerCase(Locale.ROOT);
        return switch (value) {
            case "yesterday", "last7d", "last30d" -> value;
            default -> "today";
        };
    }

    private ZoneId resolveZoneId(String timezone) {
        try {
            return ZoneId.of(StringUtils.defaultIfBlank(timezone, "UTC"));
        } catch (Exception ex) {
            return ZoneId.of("UTC");
        }
    }

    private Instant startOfDay(LocalDate date, ZoneId zoneId) {
        return date.atStartOfDay(zoneId).toInstant();
    }

    private BigDecimal successRate(Long successCount, Long failedCount, Long closedCount) {
        long denominator = defaultLong(successCount) + defaultLong(failedCount) + defaultLong(closedCount);
        if (denominator <= 0) {
            return null;
        }
        return BigDecimal.valueOf(defaultLong(successCount))
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), RATE_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private long defaultLong(Long value) {
        return value == null ? 0L : value;
    }

    private record RangeWindow(String range, Instant start, Instant end, Instant compareStart, Instant compareEnd) {
    }
}
