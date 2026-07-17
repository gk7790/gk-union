package com.gk.payment.state;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.gk.payment.domain.enums.PayDirectionEnum;
import com.gk.infra.utils.AsynUtils;
import com.gk.ledger.posting.LedgerPostingResult;
import com.gk.payment.dao.PayoutOrderDao;
import com.gk.payment.entity.PayoutOrderEntity;
import com.gk.payment.enums.MerchantOrderStatusEnum;
import com.gk.payment.enums.PayoutOrderStatusEnum;
import com.gk.payment.service.OrderStatusLogService;
import com.gk.psp.callback.model.PspCallbackOrder;
import com.gk.psp.callback.model.PspCallbackResult;
import com.gk.psp.callback.support.PspCallbackUtils;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.function.Consumer;

/**
 * 统一收口 payout_order 的状态变更和状态日志。
 *
 * <p>调用方仍然负责业务编排、PSP 调用和账务入账；本服务只负责订单状态如何流转、哪些字段要一起更新、
 * 以及状态变更日志如何记录。</p>
 */
@Service
@RequiredArgsConstructor
public class PayoutOrderStateService {
    private final PayoutOrderDao payoutOrderDao;
    private final OrderStatusLogService orderStatusLogService;

    /**
     * 应用 PSP 非终态结果，订单保持处理中，后续继续等待回调或主动查单推进。
     */
    public boolean applyProcessingResult(PspCallbackOrder order, PspCallbackResult result, OrderStateChangeContext context) {
        String toStatus = PayoutOrderStatusEnum.PROCESSING.code();
        boolean updated = updateProcessingEligible(order.id(), wrapper -> {
            applyCommon(wrapper, toStatus, result, order);
            applyMerchantStatus(wrapper, toStatus);
        });
        if (updated) {
            recordChange(order, order.status(), toStatus, context);
        }
        return updated;
    }

    /**
     * 应用 PSP 回调或主动查单得到的终态结果。
     *
     * <p>SUCCESS 表示冻结资金被正式扣减，写入 {@code success_journal_no}；
     * FAILED 表示冻结资金被释放，写入 {@code release_journal_no}。
     * 账务动作由调用方先执行，再把 {@code postingResult} 传进来。</p>
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
            applyMerchantStatus(wrapper, targetStatus);
            String journalNo = postingResult == null ? null : postingResult.getJournalNo();
            Instant now = Instant.now();
            if (PayoutOrderStatusEnum.SUCCESS.code().equals(targetStatus)) {
                // 代付成功表示之前冻结的资金已经被正式扣减。
                wrapper.set("completed_at", now)
                        .set(journalNo != null, "success_journal_no", journalNo);
            } else {
                String failMsg = StringUtils.left(result.getErrorMessage(), 512);
                // 代付失败表示之前冻结的资金需要释放，不是结算释放。
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
     * 在订单状态已经更新成功后，补写账务流水号。
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
            // 这里是代付失败后的解冻流水，不是结算流水。
            wrapper.set("release_journal_no", journalNo);
        }
        return payoutOrderDao.update(null, wrapper) > 0;
    }

    /**
     * 停止自动 PSP 查单/提交，把订单留给人工处理。
     */
    public boolean markManualReview(PayoutOrderEntity order, String reason) {
        UpdateWrapper<PayoutOrderEntity> wrapper = new UpdateWrapper<>();
        wrapper.eq("id", order.getId())
                .eq("status", PayoutOrderStatusEnum.PROCESSING.code())
                .set("status", PayoutOrderStatusEnum.MANUAL_REVIEW.code())
                .set("status_reason", reason)
                .set("merchant_status_code", MerchantOrderStatusEnum.PROCESSING.code())
                .set("merchant_status_reason", MerchantOrderStatusEnum.PROCESSING.statusReason())
                .set("next_query_at", null);
        if (payoutOrderDao.update(null, wrapper) == 0) {
            return false;
        }
        recordChange(order, order.getStatus(), PayoutOrderStatusEnum.MANUAL_REVIEW.code(),
                OrderStateChangeContext.system("PAYOUT_MANUAL_REVIEW", reason));
        return true;
    }

    /**
     * 标记代付提交 PSP 前，商户资金已经冻结成功。
     */
    public void markFrozen(PayoutOrderEntity order, LedgerPostingResult result, OrderStateChangeContext context) {
        String fromStatus = order.getStatus();
        // holdNo 是后续扣冻结或释放冻结的幂等锚点。
        order.setHoldNo(result.getHoldNo());
        order.setFreezeJournalNo(result.getJournalNo());
        order.setStatus(PayoutOrderStatusEnum.FROZEN.code());
        order.setStatusReason(null);
        order.setMerchantStatusCode(MerchantOrderStatusEnum.PROCESSING.code());
        order.setMerchantStatusReason(MerchantOrderStatusEnum.PROCESSING.statusReason());
        payoutOrderDao.updateById(order);
        recordChange(order, fromStatus, order.getStatus(), context);
    }

    /**
     * 标记 PSP 提交前的明确业务失败，常见场景是冻结余额不足。
     */
    public void markFreezeFailed(PayoutOrderEntity order, String message, String failCode, OrderStateChangeContext context) {
        String fromStatus = order.getStatus();
        order.setStatus(PayoutOrderStatusEnum.FAILED.code());
        order.setPspStatus(PayoutOrderStatusEnum.FAILED.code());
        order.setStatusReason(message);
        order.setMerchantStatusCode(MerchantOrderStatusEnum.FAILED.code());
        order.setMerchantStatusReason(MerchantOrderStatusEnum.insufficientBalanceReason());
        order.setFailCode(failCode);
        order.setFailMsg(StringUtils.left(message, 512));
        order.setFailedAt(Instant.now());
        payoutOrderDao.updateById(order);
        recordChange(order, fromStatus, order.getStatus(), context);
    }

    /**
     * 标记 PSP 提交结果未知。资金继续保持冻结，直到回调或查单确认成功/失败。
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
        order.setMerchantStatusCode(MerchantOrderStatusEnum.PROCESSING.code());
        order.setMerchantStatusReason(MerchantOrderStatusEnum.PROCESSING.statusReason());
        order.setFailCode(failCode);
        order.setFailMsg(StringUtils.left(failMessage, 512));
        if (order.getSubmittedAt() == null) {
            // UNKNOWN 可能发生在受理响应解析前，因此这里也要初始化查单窗口。
            order.setSubmittedAt(Instant.now());
        }
        order.setNextQueryAt(nextQueryAt);
        payoutOrderDao.updateById(order);
        recordChange(order, fromStatus, order.getStatus(), context);
    }

    /**
     * 异步记录状态变更日志，避免订单主状态更新等待日志落库。
     */
    public void recordChange(PayoutOrderEntity order, String fromStatus, String toStatus, OrderStateChangeContext context) {
        OrderStateChangeContext safeContext = context == null ? OrderStateChangeContext.system(toStatus, null) : context;
        executeStatusLog(() -> orderStatusLogService.recordChange(
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
     * 只更新非终态订单，防止重复回调或延迟查单覆盖已终态订单。
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
     * 只允许非终态结果更新正常处理中的订单，禁止延迟查单把人工审核状态改回处理中。
     */
    private boolean updateProcessingEligible(Long orderId, Consumer<UpdateWrapper<PayoutOrderEntity>> setter) {
        UpdateWrapper<PayoutOrderEntity> wrapper = new UpdateWrapper<>();
        wrapper.eq("id", orderId)
                .in("status",
                        PayoutOrderStatusEnum.CREATED.code(),
                        PayoutOrderStatusEnum.FROZEN.code(),
                        PayoutOrderStatusEnum.PROCESSING.code());
        setter.accept(wrapper);
        return payoutOrderDao.update(null, wrapper) > 0;
    }

    /**
     * 应用回调和主动查单都会写入的 PSP 通用字段。
     */
    private void applyCommon(UpdateWrapper<PayoutOrderEntity> wrapper, String status, PspCallbackResult result, PspCallbackOrder order) {
        String pspOrderNo = StringUtils.defaultIfBlank(result.getPspOrderNo(), order.pspOrderNo());
        wrapper.set("status", status)
                .set("psp_status", status)
                .set(result.getPspStatus() != null, "psp_raw_status", result.getPspStatus())
                .set(pspOrderNo != null, "psp_order_no", pspOrderNo);
    }

    private void applyMerchantStatus(UpdateWrapper<PayoutOrderEntity> wrapper, String targetStatus) {
        MerchantOrderStatusEnum merchantStatus = MerchantOrderStatusEnum.defaultByOrderStatus(targetStatus);
        if (merchantStatus == null) {
            return;
        }
        wrapper.set("merchant_status_code", merchantStatus.code())
                .set("merchant_status_reason", merchantStatus.statusReason());
    }

    /**
     * 异步记录来自回调/查单订单快照的状态变更。
     */
    private void recordChange(PspCallbackOrder order, String fromStatus, String toStatus, OrderStateChangeContext context) {
        OrderStateChangeContext safeContext = context == null ? OrderStateChangeContext.system(toStatus, null) : context;
        executeStatusLog(() -> orderStatusLogService.recordChange(
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

    private void executeStatusLog(Runnable action) {
        if (TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    AsynUtils.execute("Order status log", action);
                }
            });
            return;
        }
        AsynUtils.execute("Order status log", action);
    }
}
