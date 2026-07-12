package com.gk.payment.state;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.gk.payment.domain.enums.PayDirectionEnum;
import com.gk.infra.utils.AsynUtils;
import com.gk.ledger.posting.LedgerPostingResult;
import com.gk.payment.dao.PayinOrderDao;
import com.gk.payment.entity.PayinOrderEntity;
import com.gk.payment.enums.MerchantOrderStatusEnum;
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

/**
 * 统一收口 payin_order 的状态变更和状态日志。
 *
 * <p>调用方仍然负责业务编排、PSP 调用和账务入账；本服务只负责代收订单状态如何流转、
 * 哪些字段要一起更新，以及状态变更日志如何记录。</p>
 */
@Service
@RequiredArgsConstructor
public class PayinOrderStateService {
    private final PayinOrderDao payinOrderDao;
    private final OrderStatusLogService orderStatusLogService;

    /**
     * 应用 PSP 非终态结果，订单保持处理中，后续继续等待回调或主动查单推进。
     */
    public boolean applyProcessingResult(PspCallbackOrder order, PspCallbackResult result, OrderStateChangeContext context) {
        String toStatus = PayinOrderStatusEnum.PROCESSING.code();
        boolean updated = updateActive(order.id(), wrapper -> {
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
     * <p>SUCCESS 表示代收成功，资金进入商户待结算账户，写入 {@code ledger_journal_no}；
     * FAILED 表示代收失败，结算状态取消。账务动作由调用方先执行，再把 {@code postingResult} 传进来。</p>
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
            if (PayinOrderStatusEnum.SUCCESS.code().equals(targetStatus)) {
                // 代收成功后先进入待结算，后续由结算释放流程转入可用余额。
                wrapper.set("paid_amount", PspCallbackUtils.defaultAmount(result.getAmount(), order.amount()))
                        .set("paid_at", now)
                        .set("settle_status", SettleStatusEnum.PENDING.code())
                        .set(journalNo != null, "ledger_journal_no", journalNo);
            } else {
                // 代收失败没有待结算资金，结算状态需要取消。
                wrapper.set("failed_at", now)
                        .set("settle_status", SettleStatusEnum.CANCELLED.code());
            }
        });
        if (updated) {
            recordChange(order, order.status(), targetStatus, context);
        }
        return updated;
    }

    /**
     * 在订单状态已经更新成功后，补写代收成功入账流水号。
     */
    public boolean attachLedgerJournal(Long orderId, LedgerPostingResult postingResult) {
        String journalNo = postingResult == null ? null : postingResult.getJournalNo();
        if (StringUtils.isBlank(journalNo)) {
            return true;
        }
        UpdateWrapper<PayinOrderEntity> wrapper = new UpdateWrapper<>();
        wrapper.eq("id", orderId).set("ledger_journal_no", journalNo);
        return payinOrderDao.update(null, wrapper) > 0;
    }

    /**
     * 停止自动 PSP 查单，把订单留给人工处理。
     */
    public boolean markManualReview(PayinOrderEntity order, String reason) {
        UpdateWrapper<PayinOrderEntity> wrapper = new UpdateWrapper<>();
        wrapper.eq("id", order.getId())
                .eq("status", PayinOrderStatusEnum.PROCESSING.code())
                .set("status", PayinOrderStatusEnum.MANUAL_REVIEW.code())
                .set("status_reason", reason)
                .set("merchant_status_code", MerchantOrderStatusEnum.PROCESSING.code())
                .set("merchant_status_reason", MerchantOrderStatusEnum.PROCESSING.statusReason())
                .set("next_query_at", null);
        if (payinOrderDao.update(null, wrapper) == 0) {
            return false;
        }
        recordChange(order, order.getStatus(), PayinOrderStatusEnum.MANUAL_REVIEW.code(),
                OrderStateChangeContext.system("PAYIN_MANUAL_REVIEW", reason));
        return true;
    }

    /**
     * 关闭超时未支付订单。只有未支付且支付金额为空或为 0 的订单才允许关闭。
     */
    public boolean closeExpired(PayinOrderEntity order, Instant now, String reason) {
        UpdateWrapper<PayinOrderEntity> wrapper = new UpdateWrapper<>();
        wrapper.eq("id", order.getId())
                .in("status", PayinOrderStatusEnum.CREATED.code(), PayinOrderStatusEnum.PROCESSING.code())
                .isNull("paid_at")
                .and(item -> item.isNull("paid_amount").or().eq("paid_amount", java.math.BigDecimal.ZERO))
                .set("status", PayinOrderStatusEnum.CLOSED.code())
                .set("status_reason", reason)
                .set("merchant_status_code", MerchantOrderStatusEnum.CLOSED.code())
                .set("merchant_status_reason", MerchantOrderStatusEnum.CLOSED.statusReason())
                .set("closed_at", now)
                .set("next_query_at", null);
        if (payinOrderDao.update(null, wrapper) == 0) {
            return false;
        }
        recordChange(order, order.getStatus(), PayinOrderStatusEnum.CLOSED.code(),
                OrderStateChangeContext.system("ORDER_EXPIRED", reason));
        return true;
    }

    /**
     * 只更新非终态订单，防止重复回调或延迟查单覆盖已终态订单。
     */
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

    /**
     * 应用回调和主动查单都会写入的 PSP 通用字段。
     */
    private void applyCommon(UpdateWrapper<PayinOrderEntity> wrapper, String status, PspCallbackResult result, PspCallbackOrder order) {
        String pspOrderNo = StringUtils.defaultIfBlank(result.getPspOrderNo(), order.pspOrderNo());
        wrapper.set("status", status)
                .set("psp_status", status)
                .set(result.getPspStatus() != null, "psp_raw_status", result.getPspStatus())
                .set(pspOrderNo != null, "psp_order_no", pspOrderNo);
    }

    private void applyMerchantStatus(UpdateWrapper<PayinOrderEntity> wrapper, String targetStatus) {
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
        recordChange(order.tenantId(), order.merchantId(), order.id(), order.orderNo(), order.merchantOrderNo(),
                fromStatus, toStatus, context);
    }

    /**
     * 异步记录来自订单实体的状态变更。
     */
    private void recordChange(PayinOrderEntity order, String fromStatus, String toStatus, OrderStateChangeContext context) {
        recordChange(order.getTenantId(), order.getMerchantId(), order.getId(), order.getPayinOrderNo(),
                order.getMerchantOrderNo(), fromStatus, toStatus, context);
    }

    /**
     * 异步记录状态变更日志，避免订单主状态更新等待日志落库。
     */
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
