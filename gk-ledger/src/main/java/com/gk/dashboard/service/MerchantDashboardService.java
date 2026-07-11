package com.gk.dashboard.service;

import com.gk.common.context.ReqContextHolder;
import com.gk.common.enums.PayDirectionEnum;
import com.gk.common.enums.SubjectTypeEnum;
import com.gk.common.exception.ErrorCode;
import com.gk.common.exception.GkException;
import com.gk.dashboard.dao.MerchantDashboardDao;
import com.gk.dashboard.dto.TenantDashboardRecentOrderDTO;
import com.gk.dashboard.dto.TenantDashboardSummaryDTO;
import com.gk.dashboard.dto.TenantDashboardTodoDTO;
import com.gk.dashboard.dto.TenantDashboardTrendDTO;
import com.gk.dashboard.support.DashboardSupport;
import com.gk.dashboard.support.DashboardSupport.RangeWindow;
import com.gk.merchant.dto.MerchantDTO;
import com.gk.merchant.service.MerchantService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;

import static com.gk.dashboard.support.DashboardSupport.*;

@Service
@RequiredArgsConstructor
public class MerchantDashboardService {

    private final MerchantDashboardDao merchantDashboardDao;
    private final MerchantService merchantService;

    public TenantDashboardSummaryDTO summary(String range, String currency, boolean compare) {
        MerchantDTO merchant = loadMerchant();
        String resolvedCurrency = resolveCurrency(currency, merchant.getDefaultCurrency());
        RangeWindow window = resolveRange(range, resolveTimezone(merchant), compare);

        TenantDashboardSummaryDTO.OrderMetric payIn = normalizeOrderMetric(
                merchantDashboardDao.selectPayinOrderStats(merchant.getTenantId(), merchant.getId(), resolvedCurrency, window.start(), window.end()),
                true);
        TenantDashboardSummaryDTO.OrderMetric payOut = normalizeOrderMetric(
                merchantDashboardDao.selectPayoutOrderStats(merchant.getTenantId(), merchant.getId(), resolvedCurrency, window.start(), window.end()),
                false);

        TenantDashboardSummaryDTO summary = new TenantDashboardSummaryDTO();
        summary.setMeta(buildMeta(merchant, resolvedCurrency, window));
        summary.setPayIn(payIn);
        summary.setPayOut(payOut);
        if (compare) {
            TenantDashboardSummaryDTO.OrderMetric payInPrev = normalizeOrderMetric(
                    merchantDashboardDao.selectPayinOrderStats(merchant.getTenantId(), merchant.getId(), resolvedCurrency, window.compareStart(), window.compareEnd()),
                    true);
            TenantDashboardSummaryDTO.OrderMetric payOutPrev = normalizeOrderMetric(
                    merchantDashboardDao.selectPayoutOrderStats(merchant.getTenantId(), merchant.getId(), resolvedCurrency, window.compareStart(), window.compareEnd()),
                    false);
            summary.setPayInCompare(compareMetric(payIn, payInPrev));
            summary.setPayOutCompare(compareMetric(payOut, payOutPrev));
        }
        summary.setBalance(normalizeBalance(
                merchantDashboardDao.selectBalanceSummary(merchant.getTenantId(), merchant.getId(), resolvedCurrency)));
        summary.setTodos(normalizeTodo(
                merchantDashboardDao.selectTodoCounts(merchant.getTenantId(), merchant.getId(), resolvedCurrency)));
        return summary;
    }

    public TenantDashboardTrendDTO trend(String range, String currency) {
        MerchantDTO merchant = loadMerchant();
        String resolvedCurrency = resolveCurrency(currency, merchant.getDefaultCurrency());
        String timezone = resolveTimezone(merchant);
        ZoneId zoneId = resolveZoneId(timezone);
        RangeWindow window = resolveRange(normalizeTrendRange(range), timezone, false);
        String tzOffset = resolveTzOffset(zoneId, LocalDate.ofInstant(window.start(), zoneId));

        List<TenantDashboardTrendDTO.TrendPoint> payTrend = merchantDashboardDao.selectPayTrend(
                merchant.getTenantId(), merchant.getId(), resolvedCurrency, window.start(), window.end(), tzOffset);
        List<TenantDashboardTrendDTO.TrendPoint> payoutTrend = merchantDashboardDao.selectPayoutTrend(
                merchant.getTenantId(), merchant.getId(), resolvedCurrency, window.start(), window.end(), tzOffset);

        TenantDashboardTrendDTO trend = new TenantDashboardTrendDTO();
        trend.setCurrency(resolvedCurrency);
        trend.setRange(window.range());
        trend.setTimezone(timezone);
        trend.setPoints(mergeTrendPoints(window, zoneId, payTrend, payoutTrend));
        return trend;
    }

    public TenantDashboardTodoDTO todos(String type, String currency, int limit) {
        String todoType = parseTodoType(type);
        if (todoType == null) {
            throw new GkException(ErrorCode.BAD_REQUEST);
        }

        MerchantDTO merchant = loadMerchant();
        String resolvedCurrency = resolveCurrency(currency, merchant.getDefaultCurrency());
        int resolvedLimit = Math.min(Math.max(limit, 1), TODO_LIMIT_MAX);
        List<TenantDashboardTodoDTO.TodoItem> items = merchantDashboardDao.listTodos(
                merchant.getTenantId(), merchant.getId(), resolvedCurrency, todoType, resolvedLimit);
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

    public TenantDashboardRecentOrderDTO recentOrders(String bizType, String currency, int limit) {
        String resolvedBizType = parseBizType(bizType);
        if (resolvedBizType == null) {
            throw new GkException(ErrorCode.BAD_REQUEST);
        }

        MerchantDTO merchant = loadMerchant();
        String resolvedCurrency = resolveCurrency(currency, merchant.getDefaultCurrency());
        int resolvedLimit = Math.min(Math.max(limit, 1), RECENT_ORDER_LIMIT_MAX);
        List<TenantDashboardRecentOrderDTO.RecentOrder> items = PayDirectionEnum.PAYIN.matches(resolvedBizType)
                ? merchantDashboardDao.selectRecentPayinOrders(merchant.getTenantId(), merchant.getId(), resolvedCurrency, resolvedLimit)
                : merchantDashboardDao.selectRecentPayoutOrders(merchant.getTenantId(), merchant.getId(), resolvedCurrency, resolvedLimit);
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

    private MerchantDTO loadMerchant() {
        assertMerchantScope();
        Long tenantId = ReqContextHolder.getTenantId();
        Long merchantId = ReqContextHolder.getMerchantId();
        MerchantDTO merchant = merchantService.get(merchantId);
        if (merchant == null || merchant.getId() == null) {
            throw new GkException(ErrorCode.NOT_FOUND);
        }
        if (!tenantId.equals(merchant.getTenantId())) {
            throw new GkException(ErrorCode.FORBIDDEN);
        }
        return merchant;
    }

    private void assertMerchantScope() {
        if (!SubjectTypeEnum.MERCHANT.matches(ReqContextHolder.getSubjectType())) {
            throw new GkException(ErrorCode.FORBIDDEN);
        }
        if (ReqContextHolder.getTenantId() == null || ReqContextHolder.getMerchantId() == null) {
            throw new GkException(ErrorCode.DATA_SCOPE_PARAMS_ERROR);
        }
    }

    private String resolveCurrency(String requestCurrency, String defaultCurrency) {
        String value = StringUtils.defaultIfBlank(requestCurrency, defaultCurrency);
        if (StringUtils.isBlank(value)) {
            throw new GkException(ErrorCode.BAD_REQUEST);
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String resolveTimezone(MerchantDTO merchant) {
        return StringUtils.defaultIfBlank(merchant.getTimezone(), "UTC");
    }

    private TenantDashboardSummaryDTO.Meta buildMeta(MerchantDTO merchant, String currency, RangeWindow window) {
        TenantDashboardSummaryDTO.Meta meta = new TenantDashboardSummaryDTO.Meta();
        meta.setTenantId(merchant.getTenantId());
        meta.setMerchantId(merchant.getId());
        meta.setMerchantName(merchant.getMerchantName());
        meta.setMerchantNo(merchant.getMerchantNo());
        meta.setTimezone(resolveTimezone(merchant));
        meta.setCurrency(currency);
        meta.setRange(window.range());
        meta.setRangeStart(window.start());
        meta.setRangeEnd(window.end());
        meta.setCompareRangeStart(window.compareStart());
        meta.setCompareRangeEnd(window.compareEnd());
        return meta;
    }

    private String parseTodoType(String type) {
        if (StringUtils.isBlank(type)) {
            return null;
        }
        return switch (type.trim()) {
            case "NOTIFY_FAILED", "notifyFailed" -> "NOTIFY_FAILED";
            case "PROCESSING_PAYIN", "processingPayin" -> "PROCESSING_PAYIN";
            case "PROCESSING_PAYOUT", "processingPayout" -> "PROCESSING_PAYOUT";
            default -> null;
        };
    }

    private void normalizeTodoItem(TenantDashboardTodoDTO.TodoItem item) {
        item.setAmount(money(item.getAmount()));
        if (PayDirectionEnum.PAYIN.matches(item.getBizType())) {
            item.setRoutePath("/payment/payin-order/detail?id=" + item.getOrderId());
        } else if (PayDirectionEnum.PAYOUT.matches(item.getBizType())) {
            item.setRoutePath("/payment/payout-order/detail?id=" + item.getOrderId());
        }
    }

    private void normalizeRecentOrder(TenantDashboardRecentOrderDTO.RecentOrder item, String bizType) {
        item.setAmount(money(item.getAmount()));
        if (PayDirectionEnum.PAYIN.matches(bizType)) {
            item.setRoutePath("/payment/payin-order/detail?id=" + item.getOrderId());
        } else {
            item.setRoutePath("/payment/payout-order/detail?id=" + item.getOrderId());
        }
    }
}
