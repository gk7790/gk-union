package com.gk.payment.state;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.gk.common.enums.PayDirectionEnum;
import com.gk.infra.utils.AsynUtils;
import com.gk.ledger.posting.LedgerPostingResult;
import com.gk.payment.dao.PayoutOrderDao;
import com.gk.payment.entity.PayoutOrderEntity;
import com.gk.payment.enums.PayoutOrderStatusEnum;
import com.gk.payment.service.OrderStatusLogService;
import com.gk.psp.callback.model.PspCallbackOrder;
import com.gk.psp.callback.model.PspCallbackResult;
import com.gk.psp.callback.support.PspCallbackUtils;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.function.Consumer;

/**
 * Centralizes payout_order state mutations and status-log writing.
 *
 * <p>Callers still own business orchestration, PSP calls, and ledger posting. This service owns how a payout order is
 * moved between states, which columns are updated together, and how status changes are logged.</p>
 */
@Service
@RequiredArgsConstructor
public class PayoutOrderStateService {
    private final PayoutOrderDao payoutOrderDao;
    private final OrderStatusLogService orderStatusLogService;

    /**
     * Applies a non-terminal PSP result. This keeps the order queryable and eligible for later callback/query progress.
     */
    public boolean applyProcessingResult(PspCallbackOrder order, PspCallbackResult result, OrderStateChangeContext context) {
        String toStatus = PayoutOrderStatusEnum.PROCESSING.code();
        boolean updated = updateActive(order.id(), wrapper -> applyCommon(wrapper, toStatus, result, order));
        if (updated) {
            recordChange(order, order.status(), toStatus, context);
        }
        return updated;
    }

    /**
     * Applies a terminal PSP result from callback or active query.
     *
     * <p>SUCCESS consumes frozen funds and writes {@code success_journal_no}; FAILED releases frozen funds and writes
     * {@code release_journal_no}. The ledger operation is executed by the caller before passing {@code postingResult}.</p>
     */
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
            if (PayoutOrderStatusEnum.SUCCESS.code().equals(targetStatus)) {
                // Payout success means the previously frozen funds have been consumed.
                wrapper.set("completed_at", now)
                        .set(journalNo != null, "success_journal_no", journalNo);
            } else {
                String failMsg = StringUtils.left(result.getErrorMessage(), 512);
                // Payout failure means the previously frozen funds should be released, not settled.
                wrapper.set("failed_at", now)
                        .set(result.getErrorCode() != null, "fail_code", result.getErrorCode())
                        .set(failMsg != null, "fail_msg", failMsg)
                        .set(journalNo != null, "release_journal_no", journalNo);
            }
        });
        if (updated) {
            recordChange(order, order.status(), targetStatus, context);
        }
        return updated;
    }

    /**
     * Back-fills the ledger journal after the status update has already succeeded.
     */
    public boolean attachLedgerJournal(Long orderId, String targetStatus, LedgerPostingResult postingResult) {
        String journalNo = postingResult == null ? null : postingResult.getJournalNo();
        if (StringUtils.isBlank(journalNo)) {
            return true;
        }
        String normalizedStatus = PspCallbackUtils.normalizeStatus(targetStatus);
        UpdateWrapper<PayoutOrderEntity> wrapper = new UpdateWrapper<>();
        wrapper.eq("id", orderId);
        if (PayoutOrderStatusEnum.SUCCESS.code().equals(normalizedStatus)) {
            wrapper.set("success_journal_no", journalNo);
        } else if (PayoutOrderStatusEnum.FAILED.code().equals(normalizedStatus)) {
            // This is the unfreeze journal for failed payout, not a settlement journal.
            wrapper.set("release_journal_no", journalNo);
        }
        return payoutOrderDao.update(null, wrapper) > 0;
    }

    /**
     * Stops automatic PSP querying/submission and leaves the order for manual handling.
     */
    public boolean markManualReview(PayoutOrderEntity order, String reason) {
        UpdateWrapper<PayoutOrderEntity> wrapper = new UpdateWrapper<>();
        wrapper.eq("id", order.getId())
                .eq("status", PayoutOrderStatusEnum.PROCESSING.code())
                .set("status", PayoutOrderStatusEnum.MANUAL_REVIEW.code())
                .set("status_reason", reason)
                .set("next_query_at", null);
        if (payoutOrderDao.update(null, wrapper) == 0) {
            return false;
        }
        recordChange(order, order.getStatus(), PayoutOrderStatusEnum.MANUAL_REVIEW.code(),
                OrderStateChangeContext.system("PAYOUT_MANUAL_REVIEW", reason));
        return true;
    }

    /**
     * Marks that merchant funds were frozen successfully before submitting the payout to PSP.
     */
    public boolean markFrozen(PayoutOrderEntity order, LedgerPostingResult result, OrderStateChangeContext context) {
        String fromStatus = order.getStatus();
        // holdNo is the idempotency anchor for later consume/release operations.
        order.setHoldNo(result.getHoldNo());
        order.setFreezeJournalNo(result.getJournalNo());
        order.setStatus(PayoutOrderStatusEnum.FROZEN.code());
        order.setStatusReason(null);
        payoutOrderDao.updateById(order);
        recordChange(order, fromStatus, order.getStatus(), context);
        return true;
    }

    /**
     * Marks that PSP accepted the payout submission. This is not final success.
     */
    public void markPspAccepted(PayoutOrderEntity order,
                                String pspRequestNo,
                                String pspOrderNo,
                                String pspRawStatus,
                                Instant nextQueryAt,
                                OrderStateChangeContext context) {
        String fromStatus = order.getStatus();
        order.setPspRequestNo(pspRequestNo);
        order.setPspOrderNo(pspOrderNo);
        order.setPspRawStatus(pspRawStatus);
        order.setStatus(PayoutOrderStatusEnum.PROCESSING.code());
        order.setPspStatus(PayoutOrderStatusEnum.PROCESSING.code());
        // submittedAt starts the SLA/query window for this payout.
        order.setSubmittedAt(Instant.now());
        order.setNextQueryAt(nextQueryAt);
        payoutOrderDao.updateById(order);
        recordChange(order, fromStatus, order.getStatus(), context);
    }

    /**
     * Marks a clear PSP rejection during submit. Frozen funds should already be released by the caller.
     */
    public void markSubmitRejected(PayoutOrderEntity order,
                                   String pspRequestNo,
                                   String pspOrderNo,
                                   String pspRawStatus,
                                   String failCode,
                                   String reason,
                                   OrderStateChangeContext context) {
        String fromStatus = order.getStatus();
        order.setPspRequestNo(pspRequestNo);
        order.setPspOrderNo(pspOrderNo);
        order.setPspRawStatus(pspRawStatus);
        order.setStatus(PayoutOrderStatusEnum.FAILED.code());
        order.setPspStatus(PayoutOrderStatusEnum.FAILED.code());
        order.setFailCode(failCode);
        order.setFailMsg(StringUtils.left(reason, 512));
        order.setStatusReason(reason);
        order.setFailedAt(Instant.now());
        payoutOrderDao.updateById(order);
        recordChange(order, fromStatus, order.getStatus(), context);
    }

    /**
     * Marks a business failure before PSP submission, usually insufficient balance during freeze.
     */
    public void markFreezeFailed(PayoutOrderEntity order, String message, String failCode, OrderStateChangeContext context) {
        String fromStatus = order.getStatus();
        order.setStatus(PayoutOrderStatusEnum.FAILED.code());
        order.setPspStatus(PayoutOrderStatusEnum.FAILED.code());
        order.setStatusReason(message);
        order.setFailCode(failCode);
        order.setFailMsg(StringUtils.left(message, 512));
        order.setFailedAt(Instant.now());
        payoutOrderDao.updateById(order);
        recordChange(order, fromStatus, order.getStatus(), context);
    }

    /**
     * Marks an unknown PSP submit result. Funds stay frozen until callback/query proves success or failure.
     */
    public void markSubmitUnknown(PayoutOrderEntity order,
                                  String failCode,
                                  String failMessage,
                                  String statusReason,
                                  Instant nextQueryAt,
                                  OrderStateChangeContext context) {
        String fromStatus = order.getStatus();
        order.setStatus(PayoutOrderStatusEnum.PROCESSING.code());
        order.setPspStatus(PayoutOrderStatusEnum.PROCESSING.code());
        order.setStatusReason(statusReason);
        order.setFailCode(failCode);
        order.setFailMsg(StringUtils.left(failMessage, 512));
        if (order.getSubmittedAt() == null) {
            // UNKNOWN may happen before an accepted response is parsed, so initialize the query window here.
            order.setSubmittedAt(Instant.now());
        }
        order.setNextQueryAt(nextQueryAt);
        payoutOrderDao.updateById(order);
        recordChange(order, fromStatus, order.getStatus(), context);
    }

    /**
     * Records status changes asynchronously so order state mutations do not wait on log insert latency.
     */
    public void recordChange(PayoutOrderEntity order, String fromStatus, String toStatus, OrderStateChangeContext context) {
        OrderStateChangeContext safeContext = context == null ? OrderStateChangeContext.system(toStatus, null) : context;
        AsynUtils.execute("Order status log", () -> orderStatusLogService.recordChange(
                    PayDirectionEnum.PAYOUT.code(),
                    order.getTenantId(),
                    order.getMerchantId(),
                    order.getId(),
                    order.getPayoutOrderNo(),
                    fromStatus,
                    toStatus,
                    safeContext.eventType(),
                    safeContext.reason(),
                    safeContext.operatorType(),
                    safeContext.operatorId(),
                    StringUtils.defaultIfBlank(safeContext.requestId(), order.getMerchantOrderNo()),
                    safeContext.traceId()
            ));
    }

    /**
     * Updates only non-final states to protect terminal orders from duplicate callbacks or delayed queries.
     */
    private boolean updateActive(Long orderId, Consumer<UpdateWrapper<PayoutOrderEntity>> setter) {
        UpdateWrapper<PayoutOrderEntity> wrapper = new UpdateWrapper<>();
        wrapper.eq("id", orderId)
                .in("status",
                        PayoutOrderStatusEnum.CREATED.code(),
                        PayoutOrderStatusEnum.FROZEN.code(),
                        PayoutOrderStatusEnum.PROCESSING.code(),
                        PayoutOrderStatusEnum.MANUAL_REVIEW.code());
        setter.accept(wrapper);
        return payoutOrderDao.update(null, wrapper) > 0;
    }

    /**
     * Applies PSP-facing fields common to callback and active-query results.
     */
    private void applyCommon(UpdateWrapper<PayoutOrderEntity> wrapper, String status, PspCallbackResult result, PspCallbackOrder order) {
        String pspOrderNo = StringUtils.defaultIfBlank(result.getPspOrderNo(), order.pspOrderNo());
        wrapper.set("status", status)
                .set("psp_status", status)
                .set(result.getPspStatus() != null, "psp_raw_status", result.getPspStatus())
                .set(pspOrderNo != null, "psp_order_no", pspOrderNo);
    }

    /**
     * Records status changes for callback/query snapshots asynchronously.
     */
    private void recordChange(PspCallbackOrder order, String fromStatus, String toStatus, OrderStateChangeContext context) {
        OrderStateChangeContext safeContext = context == null ? OrderStateChangeContext.system(toStatus, null) : context;
        AsynUtils.execute("Order status log", () -> orderStatusLogService.recordChange(
                    PayDirectionEnum.PAYOUT.code(),
                    order.tenantId(),
                    order.merchantId(),
                    order.id(),
                    order.orderNo(),
                    fromStatus,
                    toStatus,
                    safeContext.eventType(),
                    safeContext.reason(),
                    safeContext.operatorType(),
                    safeContext.operatorId(),
                    StringUtils.defaultIfBlank(safeContext.requestId(), order.merchantOrderNo()),
                    safeContext.traceId()
            ));
    }
}
