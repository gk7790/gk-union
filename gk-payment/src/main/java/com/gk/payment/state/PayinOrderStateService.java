package com.gk.payment.state;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.gk.common.enums.PayDirectionEnum;
import com.gk.infra.utils.AsynUtils;
import com.gk.ledger.posting.LedgerPostingResult;
import com.gk.payment.dao.PayinOrderDao;
import com.gk.payment.entity.PayinOrderEntity;
import com.gk.payment.enums.PayinOrderStatusEnum;
import com.gk.payment.enums.SettleStatusEnum;
import com.gk.payment.service.OrderStatusLogService;
import com.gk.psp.callback.model.PspCallbackOrder;
import com.gk.psp.callback.model.PspCallbackResult;
import com.gk.psp.callback.support.PspCallbackUtils;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
public class PayinOrderStateService {
    private final PayinOrderDao payinOrderDao;
    private final OrderStatusLogService orderStatusLogService;

    public boolean applyProcessingResult(PspCallbackOrder order, PspCallbackResult result, OrderStateChangeContext context) {
        String toStatus = PayinOrderStatusEnum.PROCESSING.code();
        boolean updated = updateActive(order.id(), wrapper -> applyCommon(wrapper, toStatus, result, order));
        if (updated) {
            recordChange(order, order.status(), toStatus, context);
        }
        return updated;
    }

    public boolean applyTerminalResult(PspCallbackOrder order,
                                       PspCallbackResult result,
                                       String targetStatus,
                                       LedgerPostingResult postingResult,
                                       OrderStateChangeContext context) {
        boolean updated = updateActive(order.id(), wrapper -> {
            applyCommon(wrapper, targetStatus, result, order);
            String statusReason = result.getErrorMessage();
            wrapper.set(statusReason != null, "status_reason", statusReason);
            String journalNo = postingResult == null ? null : postingResult.getJournalNo();
            Instant now = Instant.now();
            if (PayinOrderStatusEnum.SUCCESS.code().equals(targetStatus)) {
                wrapper.set("paid_amount", PspCallbackUtils.defaultAmount(result.getAmount(), order.amount()))
                        .set("paid_at", now)
                        .set("settle_status", SettleStatusEnum.PENDING.code())
                        .set(journalNo != null, "ledger_journal_no", journalNo);
            } else {
                wrapper.set("failed_at", now)
                        .set("settle_status", SettleStatusEnum.CANCELLED.code());
            }
        });
        if (updated) {
            recordChange(order, order.status(), targetStatus, context);
        }
        return updated;
    }

    public boolean attachLedgerJournal(Long orderId, LedgerPostingResult postingResult) {
        String journalNo = postingResult == null ? null : postingResult.getJournalNo();
        if (StringUtils.isBlank(journalNo)) {
            return true;
        }
        UpdateWrapper<PayinOrderEntity> wrapper = new UpdateWrapper<>();
        wrapper.eq("id", orderId).set("ledger_journal_no", journalNo);
        return payinOrderDao.update(null, wrapper) > 0;
    }

    public boolean markManualReview(PayinOrderEntity order, String reason) {
        UpdateWrapper<PayinOrderEntity> wrapper = new UpdateWrapper<>();
        wrapper.eq("id", order.getId())
                .eq("status", PayinOrderStatusEnum.PROCESSING.code())
                .set("status", PayinOrderStatusEnum.MANUAL_REVIEW.code())
                .set("status_reason", reason)
                .set("next_query_at", null);
        if (payinOrderDao.update(null, wrapper) == 0) {
            return false;
        }
        recordChange(order, order.getStatus(), PayinOrderStatusEnum.MANUAL_REVIEW.code(),
                OrderStateChangeContext.system("PAYIN_MANUAL_REVIEW", reason));
        return true;
    }

    public boolean closeExpired(PayinOrderEntity order, Instant now, String reason) {
        UpdateWrapper<PayinOrderEntity> wrapper = new UpdateWrapper<>();
        wrapper.eq("id", order.getId())
                .in("status", PayinOrderStatusEnum.CREATED.code(), PayinOrderStatusEnum.PROCESSING.code())
                .isNull("paid_at")
                .and(item -> item.isNull("paid_amount").or().eq("paid_amount", java.math.BigDecimal.ZERO))
                .set("status", PayinOrderStatusEnum.CLOSED.code())
                .set("status_reason", reason)
                .set("closed_at", now)
                .set("next_query_at", null);
        if (payinOrderDao.update(null, wrapper) == 0) {
            return false;
        }
        recordChange(order, order.getStatus(), PayinOrderStatusEnum.CLOSED.code(),
                OrderStateChangeContext.system("ORDER_EXPIRED", reason));
        return true;
    }

    private boolean updateActive(Long orderId, Consumer<UpdateWrapper<PayinOrderEntity>> setter) {
        UpdateWrapper<PayinOrderEntity> wrapper = new UpdateWrapper<>();
        wrapper.eq("id", orderId)
                .in("status",
                        PayinOrderStatusEnum.CREATED.code(),
                        PayinOrderStatusEnum.PROCESSING.code(),
                        PayinOrderStatusEnum.MANUAL_REVIEW.code());
        setter.accept(wrapper);
        return payinOrderDao.update(null, wrapper) > 0;
    }

    private void applyCommon(UpdateWrapper<PayinOrderEntity> wrapper, String status, PspCallbackResult result, PspCallbackOrder order) {
        String pspOrderNo = StringUtils.defaultIfBlank(result.getPspOrderNo(), order.pspOrderNo());
        wrapper.set("status", status)
                .set("psp_status", status)
                .set(result.getPspStatus() != null, "psp_raw_status", result.getPspStatus())
                .set(pspOrderNo != null, "psp_order_no", pspOrderNo);
    }

    private void recordChange(PspCallbackOrder order, String fromStatus, String toStatus, OrderStateChangeContext context) {
        recordChange(order.tenantId(), order.merchantId(), order.id(), order.orderNo(), order.merchantOrderNo(),
                fromStatus, toStatus, context);
    }

    private void recordChange(PayinOrderEntity order, String fromStatus, String toStatus, OrderStateChangeContext context) {
        recordChange(order.getTenantId(), order.getMerchantId(), order.getId(), order.getPayinOrderNo(),
                order.getMerchantOrderNo(), fromStatus, toStatus, context);
    }

    private void recordChange(Long tenantId,
                              Long merchantId,
                              Long orderId,
                              String orderNo,
                              String merchantOrderNo,
                              String fromStatus,
                              String toStatus,
                              OrderStateChangeContext context) {
        OrderStateChangeContext safeContext = context == null ? OrderStateChangeContext.system(toStatus, null) : context;
        AsynUtils.execute("Order status log", () -> orderStatusLogService.recordChange(
                    PayDirectionEnum.PAYIN.code(),
                    tenantId,
                    merchantId,
                    orderId,
                    orderNo,
                    fromStatus,
                    toStatus,
                    safeContext.eventType(),
                    safeContext.reason(),
                    safeContext.operatorType(),
                    safeContext.operatorId(),
                    StringUtils.defaultIfBlank(safeContext.requestId(), merchantOrderNo),
                    safeContext.traceId()
            ));
    }
}
