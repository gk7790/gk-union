package com.gk.payment.reconcile;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.gk.common.constant.Constant;
import com.gk.common.task.TaskExecutionRecord;
import com.gk.common.task.TaskExecutions;
import com.gk.payment.domain.enums.BizTypeEnum;
import com.gk.payment.config.PspQueryConfig;
import com.gk.payment.config.PaymentConfigService;
import com.gk.payment.dao.PayinOrderDao;
import com.gk.payment.dao.PayoutOrderDao;
import com.gk.payment.entity.PayinOrderEntity;
import com.gk.payment.entity.PayoutOrderEntity;
import com.gk.payment.enums.PayinOrderStatusEnum;
import com.gk.payment.enums.PayoutOrderStatusEnum;
import com.gk.payment.psp.PspOrderRequests;
import com.gk.psp.callback.support.PspCallbackUtils;
import com.gk.psp.query.PspOrderQueryResult;
import com.gk.payment.callback.PspCallbackOrderResolver;
import com.gk.payment.callback.PspOrderResultHandler;
import com.gk.psp.query.PspPayQueryService;
import com.gk.psp.query.PspPayoutQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PspOrderQueryExecutor {
    private static final int BATCH_SIZE = 50;
    private static final int MAX_DRAIN_LOOPS = 5;
    private static final int MAX_QUERY_COUNT = 30;
    private static final long QUERY_LEASE_SECONDS = 300L;
    private static final List<Long> BACKOFF_SECONDS = List.of(60L, 120L, 300L, 600L, 900L, 1800L);

    private final PayinOrderDao payinOrderDao;
    private final PayoutOrderDao payoutOrderDao;
    private final PspPayQueryService payQueryService;
    private final PspPayoutQueryService payoutQueryService;
    private final PspOrderResultHandler resultHandler;
    private final PspCallbackOrderResolver orderResolver;
    private final PaymentConfigService configService;

    public int drainPayinOrders() {
        int total = 0;
        for (int loop = 0; loop < MAX_DRAIN_LOOPS; loop++) {
            int handled = queryPayBatch();
            total += handled;
            if (handled < BATCH_SIZE) {
                break;
            }
        }
        return total;
    }

    public int drainPayoutOrders() {
        int total = 0;
        for (int loop = 0; loop < MAX_DRAIN_LOOPS; loop++) {
            int handled = queryPayoutBatch();
            total += handled;
            if (handled < BATCH_SIZE) {
                break;
            }
        }
        return total;
    }

    private int queryPayBatch() {
        Instant now = Instant.now();
        List<PayinOrderEntity> orders = payinOrderDao.selectList(new QueryWrapper<PayinOrderEntity>()
                .eq("status", PayinOrderStatusEnum.PROCESSING.code())
                .and(wrapper -> wrapper.ne("psp_code", Constant.SANDBOX).or().isNull("psp_code"))
                .and(wrapper -> wrapper.lt("query_count", maxQueryCount()).or().isNull("query_count"))
                .and(wrapper -> wrapper.le("next_query_at", now).or().isNull("next_query_at"))
                .orderByAsc("next_query_at", "id")
                .last("limit " + BATCH_SIZE));
        int handled = 0;
        for (PayinOrderEntity order : orders) {
            if (claimPay(order, now)) {
                handled++;
                queryPay(order);
            }
        }
        return handled;
    }

    private int queryPayoutBatch() {
        Instant now = Instant.now();
        List<PayoutOrderEntity> orders = payoutOrderDao.selectList(new QueryWrapper<PayoutOrderEntity>()
                .eq("status", PayoutOrderStatusEnum.PROCESSING.code())
                .and(wrapper -> wrapper.ne("psp_code", Constant.SANDBOX).or().isNull("psp_code"))
                .and(wrapper -> wrapper.lt("query_count", maxQueryCount()).or().isNull("query_count"))
                .and(wrapper -> wrapper.le("next_query_at", now).or().isNull("next_query_at"))
                .orderByAsc("next_query_at", "id")
                .last("limit " + BATCH_SIZE));
        int handled = 0;
        for (PayoutOrderEntity order : orders) {
            if (claimPayout(order, now)) {
                handled++;
                queryPayout(order);
            }
        }
        return handled;
    }

    private void queryPay(PayinOrderEntity order) {
        int attemptNo = safeCount(order.getQueryCount()) + 1;
        TaskExecutionRecord record = TaskExecutions.current().record(
                "Payin query order=" + order.getPayinOrderNo() + ", attempt=" + attemptNo);
        try {
            record.step("QUERY", "Request PSP order status");
            PspOrderQueryResult result = payQueryService.query(PspOrderRequests.fromPayinOrder(order));
            resultHandler.handle(BizTypeEnum.PAYIN_ORDER.code(), orderResolver.fromPayinOrder(order, null), result);
            if (!PspCallbackUtils.isTerminal(result.getOrderStatus())) {
                reschedulePay(order.getId(), attemptNo, null);
                record.complete("Non-terminal result, query rescheduled");
            } else {
                markPayQueryFinished(order.getId(), attemptNo);
                record.complete("Terminal result applied");
            }
        } catch (Exception ex) {
            log.warn("Pay order query failed, orderNo={}, err={}", order.getPayinOrderNo(), ex.getMessage());
            reschedulePay(order.getId(), attemptNo, ex.getMessage());
            record.error("QUERY", "PSP query failed and was rescheduled", ex);
        }
    }

    private void queryPayout(PayoutOrderEntity order) {
        int attemptNo = safeCount(order.getQueryCount()) + 1;
        TaskExecutionRecord record = TaskExecutions.current().record(
                "Payout query order=" + order.getPayoutOrderNo() + ", attempt=" + attemptNo);
        try {
            record.step("QUERY", "Request PSP order status");
            PspOrderQueryResult result = payoutQueryService.query(PspOrderRequests.fromPayoutOrder(order));
            resultHandler.handle(BizTypeEnum.PAYOUT_ORDER.code(), orderResolver.fromPayoutOrder(order, null), result);
            if (!PspCallbackUtils.isTerminal(result.getOrderStatus())) {
                reschedulePayout(order.getId(), attemptNo, null);
                record.complete("Non-terminal result, query rescheduled");
            } else {
                markPayoutQueryFinished(order.getId(), attemptNo);
                record.complete("Terminal result applied");
            }
        } catch (Exception ex) {
            log.warn("Payout order query failed, orderNo={}, err={}", order.getPayoutOrderNo(), ex.getMessage());
            reschedulePayout(order.getId(), attemptNo, ex.getMessage());
            record.error("QUERY", "PSP query failed and was rescheduled", ex);
        }
    }

    private boolean claimPay(PayinOrderEntity order, Instant now) {
        UpdateWrapper<PayinOrderEntity> wrapper = new UpdateWrapper<>();
        wrapper.eq("id", order.getId())
                .eq("status", PayinOrderStatusEnum.PROCESSING.code())
                .and(item -> item.le("next_query_at", now).or().isNull("next_query_at"))
                .and(item -> item.lt("query_count", maxQueryCount()).or().isNull("query_count"))
                .set("query_count", safeCount(order.getQueryCount()) + 1)
                .set("next_query_at", now.plusSeconds(QUERY_LEASE_SECONDS));
        addExpectedQueryCount(wrapper, order.getQueryCount());
        return payinOrderDao.update(null, wrapper) == 1;
    }

    private boolean claimPayout(PayoutOrderEntity order, Instant now) {
        UpdateWrapper<PayoutOrderEntity> wrapper = new UpdateWrapper<>();
        wrapper.eq("id", order.getId())
                .eq("status", PayoutOrderStatusEnum.PROCESSING.code())
                .and(item -> item.le("next_query_at", now).or().isNull("next_query_at"))
                .and(item -> item.lt("query_count", maxQueryCount()).or().isNull("query_count"))
                .set("query_count", safeCount(order.getQueryCount()) + 1)
                .set("next_query_at", now.plusSeconds(QUERY_LEASE_SECONDS));
        addExpectedQueryCount(wrapper, order.getQueryCount());
        return payoutOrderDao.update(null, wrapper) == 1;
    }

    private <T> void addExpectedQueryCount(UpdateWrapper<T> wrapper, Integer queryCount) {
        if (queryCount == null) {
            wrapper.isNull("query_count");
        } else {
            wrapper.eq("query_count", queryCount);
        }
    }

    private void reschedulePay(Long orderId, int attemptNo, String reason) {
        UpdateWrapper<PayinOrderEntity> wrapper = new UpdateWrapper<>();
        wrapper.eq("id", orderId)
                .eq("status", PayinOrderStatusEnum.PROCESSING.code())
                .eq("query_count", attemptNo)
                .set("query_count", attemptNo)
                .set("next_query_at", nextQueryAt(attemptNo))
                .set(StringUtils.isNotBlank(reason), "status_reason", StringUtils.left(reason, 512));
        payinOrderDao.update(null, wrapper);
    }

    private void reschedulePayout(Long orderId, int attemptNo, String reason) {
        UpdateWrapper<PayoutOrderEntity> wrapper = new UpdateWrapper<>();
        wrapper.eq("id", orderId)
                .eq("status", PayoutOrderStatusEnum.PROCESSING.code())
                .eq("query_count", attemptNo)
                .set("query_count", attemptNo)
                .set("next_query_at", nextQueryAt(attemptNo))
                .set(StringUtils.isNotBlank(reason), "status_reason", StringUtils.left(reason, 512));
        payoutOrderDao.update(null, wrapper);
    }

    private void markPayQueryFinished(Long orderId, int attemptNo) {
        UpdateWrapper<PayinOrderEntity> wrapper = new UpdateWrapper<>();
        wrapper.eq("id", orderId)
                .eq("query_count", attemptNo)
                .set("query_count", attemptNo)
                .set("next_query_at", null);
        payinOrderDao.update(null, wrapper);
    }

    private void markPayoutQueryFinished(Long orderId, int attemptNo) {
        UpdateWrapper<PayoutOrderEntity> wrapper = new UpdateWrapper<>();
        wrapper.eq("id", orderId)
                .eq("query_count", attemptNo)
                .set("query_count", attemptNo)
                .set("next_query_at", null);
        payoutOrderDao.update(null, wrapper);
    }

    private Instant nextQueryAt(int attemptNo) {
        List<Long> backoffSeconds = configService.pspQuery().getBackoffSeconds();
        if (backoffSeconds == null || backoffSeconds.isEmpty()) {
            backoffSeconds = BACKOFF_SECONDS;
        }
        int index = Math.max(0, attemptNo - 1);
        if (index >= backoffSeconds.size()) {
            index = backoffSeconds.size() - 1;
        }
        return Instant.now().plusSeconds(Math.max(1L, backoffSeconds.get(index)));
    }

    private int maxQueryCount() {
        PspQueryConfig config = configService.pspQuery();
        int maxQueryCount = config.getMaxQueryCount();
        return maxQueryCount <= 0 ? MAX_QUERY_COUNT : maxQueryCount;
    }

    private int safeCount(Integer value) {
        return value == null ? 0 : value;
    }
}
