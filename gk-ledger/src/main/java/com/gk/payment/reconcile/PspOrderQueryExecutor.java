package com.gk.payment.reconcile;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.gk.common.constant.Constant;
import com.gk.common.enums.BizTypeEnum;
import com.gk.payment.dao.PayOrderDao;
import com.gk.payment.dao.PayoutOrderDao;
import com.gk.payment.entity.PayOrderEntity;
import com.gk.payment.entity.PayoutOrderEntity;
import com.gk.payment.enums.PayOrderStatusEnum;
import com.gk.payment.enums.PayoutOrderStatusEnum;
import com.gk.payment.psp.PspOrderRequests;
import com.gk.psp.callback.model.PspCallbackOrder;
import com.gk.psp.callback.support.PspCallbackUtils;
import com.gk.psp.query.PspOrderQueryResult;
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
    private static final long[] BACKOFF_SECONDS = {60, 120, 300, 600, 900, 1800};

    private final PayOrderDao payOrderDao;
    private final PayoutOrderDao payoutOrderDao;
    private final PspPayQueryService payQueryService;
    private final PspPayoutQueryService payoutQueryService;
    private final PspOrderResultHandler resultHandler;

    public int drainPayOrders() {
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
        List<PayOrderEntity> orders = payOrderDao.selectList(new QueryWrapper<PayOrderEntity>()
                .eq("status", PayOrderStatusEnum.PROCESSING.code())
                .and(wrapper -> wrapper.ne("psp_code", Constant.SANDBOX).or().isNull("psp_code"))
                .and(wrapper -> wrapper.lt("query_count", MAX_QUERY_COUNT).or().isNull("query_count"))
                .and(wrapper -> wrapper.le("next_query_at", now).or().isNull("next_query_at"))
                .orderByAsc("next_query_at", "id")
                .last("limit " + BATCH_SIZE));
        for (PayOrderEntity order : orders) {
            queryPay(order);
        }
        return orders.size();
    }

    private int queryPayoutBatch() {
        Instant now = Instant.now();
        List<PayoutOrderEntity> orders = payoutOrderDao.selectList(new QueryWrapper<PayoutOrderEntity>()
                .eq("status", PayoutOrderStatusEnum.PROCESSING.code())
                .and(wrapper -> wrapper.ne("psp_code", Constant.SANDBOX).or().isNull("psp_code"))
                .and(wrapper -> wrapper.lt("query_count", MAX_QUERY_COUNT).or().isNull("query_count"))
                .and(wrapper -> wrapper.le("next_query_at", now).or().isNull("next_query_at"))
                .orderByAsc("next_query_at", "id")
                .last("limit " + BATCH_SIZE));
        for (PayoutOrderEntity order : orders) {
            queryPayout(order);
        }
        return orders.size();
    }

    private void queryPay(PayOrderEntity order) {
        int attemptNo = safeCount(order.getQueryCount()) + 1;
        try {
            PspOrderQueryResult result = payQueryService.query(PspOrderRequests.fromPayOrder(order));
            resultHandler.handle(BizTypeEnum.PAY_ORDER.code(), PspCallbackOrder.of(order, null), result);
            if (!PspCallbackUtils.isTerminal(result.getOrderStatus())) {
                reschedulePay(order.getId(), attemptNo, null);
            } else {
                markPayQueryFinished(order.getId(), attemptNo);
            }
        } catch (Exception ex) {
            log.warn("Pay order query failed, orderNo={}, err={}", order.getPayOrderNo(), ex.getMessage());
            reschedulePay(order.getId(), attemptNo, ex.getMessage());
        }
    }

    private void queryPayout(PayoutOrderEntity order) {
        int attemptNo = safeCount(order.getQueryCount()) + 1;
        try {
            PspOrderQueryResult result = payoutQueryService.query(PspOrderRequests.fromPayoutOrder(order));
            resultHandler.handle(BizTypeEnum.PAYOUT_ORDER.code(), PspCallbackOrder.of(order, null), result);
            if (!PspCallbackUtils.isTerminal(result.getOrderStatus())) {
                reschedulePayout(order.getId(), attemptNo, null);
            } else {
                markPayoutQueryFinished(order.getId(), attemptNo);
            }
        } catch (Exception ex) {
            log.warn("Payout order query failed, orderNo={}, err={}", order.getPayoutOrderNo(), ex.getMessage());
            reschedulePayout(order.getId(), attemptNo, ex.getMessage());
        }
    }

    private void reschedulePay(Long orderId, int attemptNo, String reason) {
        UpdateWrapper<PayOrderEntity> wrapper = new UpdateWrapper<>();
        wrapper.eq("id", orderId)
                .eq("status", PayOrderStatusEnum.PROCESSING.code())
                .set("query_count", attemptNo)
                .set("next_query_at", nextQueryAt(attemptNo))
                .set(StringUtils.isNotBlank(reason), "status_reason", StringUtils.left(reason, 512));
        payOrderDao.update(null, wrapper);
    }

    private void reschedulePayout(Long orderId, int attemptNo, String reason) {
        UpdateWrapper<PayoutOrderEntity> wrapper = new UpdateWrapper<>();
        wrapper.eq("id", orderId)
                .eq("status", PayoutOrderStatusEnum.PROCESSING.code())
                .set("query_count", attemptNo)
                .set("next_query_at", nextQueryAt(attemptNo))
                .set(StringUtils.isNotBlank(reason), "status_reason", StringUtils.left(reason, 512));
        payoutOrderDao.update(null, wrapper);
    }

    private void markPayQueryFinished(Long orderId, int attemptNo) {
        UpdateWrapper<PayOrderEntity> wrapper = new UpdateWrapper<>();
        wrapper.eq("id", orderId)
                .set("query_count", attemptNo)
                .set("next_query_at", null);
        payOrderDao.update(null, wrapper);
    }

    private void markPayoutQueryFinished(Long orderId, int attemptNo) {
        UpdateWrapper<PayoutOrderEntity> wrapper = new UpdateWrapper<>();
        wrapper.eq("id", orderId)
                .set("query_count", attemptNo)
                .set("next_query_at", null);
        payoutOrderDao.update(null, wrapper);
    }

    private Instant nextQueryAt(int attemptNo) {
        int index = Math.max(0, attemptNo - 1);
        if (index >= BACKOFF_SECONDS.length) {
            index = BACKOFF_SECONDS.length - 1;
        }
        return Instant.now().plusSeconds(BACKOFF_SECONDS[index]);
    }

    private int safeCount(Integer value) {
        return value == null ? 0 : value;
    }
}
