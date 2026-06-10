package com.gk.psp.callback.support;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gk.ledger.posting.LedgerPostingResult;
import com.gk.payment.dao.PayOrderDao;
import com.gk.payment.dao.PayoutOrderDao;
import com.gk.psp.callback.model.PspCallbackOrder;
import com.gk.psp.callback.model.PspCallbackResult;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.function.Consumer;

@Component
@RequiredArgsConstructor
public class PspCallbackOrderProcessor {
    private final PayOrderDao payOrderDao;
    private final PayoutOrderDao payoutOrderDao;

    public boolean process(String bizType, PspCallbackResult result, PspCallbackOrder order, LedgerPostingResult postingResult) {
        String targetStatus = PspCallbackUtils.normalizeStatus(result.getOrderStatus());
        if (PspCallbackConstants.STATUS_PROCESSING.equals(targetStatus)) {
            if (PspCallbackUtils.isTerminal(order.status())) {
                return false;
            }
            return update(bizType, order.id(), wrapper -> applyCommon(wrapper, PspCallbackConstants.STATUS_PROCESSING, result, order));
        }
        if (!PspCallbackUtils.isTerminal(targetStatus)) {
            throw new IllegalStateException("Unsupported callback order status");
        }
        if (targetStatus.equals(order.status()) || PspCallbackUtils.isTerminal(order.status())) {
            return false;
        }
        return update(bizType, order.id(), wrapper -> applyTerminal(bizType, wrapper, result, order, targetStatus, postingResult));
    }

    public void attachPostingResult(String bizType, Long orderId, String targetStatus, LedgerPostingResult postingResult) {
        String journalNo = postingResult == null ? null : postingResult.getJournalNo();
        if (StringUtils.isBlank(journalNo)) {
            return;
        }
        String normalizedStatus = PspCallbackUtils.normalizeStatus(targetStatus);
        boolean updated = updateJournalNo(bizType, orderId, wrapper -> {
            if (PspCallbackConstants.BIZ_TYPE_PAY_ORDER.equals(bizType)) {
                wrapper.set("ledger_journal_no", journalNo);
            } else if (PspCallbackConstants.STATUS_SUCCESS.equals(normalizedStatus)) {
                wrapper.set("success_journal_no", journalNo);
            } else if (PspCallbackConstants.STATUS_FAILED.equals(normalizedStatus)) {
                wrapper.set("release_journal_no", journalNo);
            }
        });
        if (!updated) {
            throw new IllegalStateException("Attach ledger journal to callback order failed");
        }
    }

    private void applyTerminal(String bizType, UpdateWrapper<?> wrapper, PspCallbackResult result,
                               PspCallbackOrder order, String targetStatus, LedgerPostingResult postingResult) {
        applyCommon(wrapper, targetStatus, result, order);
        String statusReason = result.getErrorMessage();
        wrapper.set(statusReason != null, "status_reason", statusReason);
        boolean success = PspCallbackConstants.STATUS_SUCCESS.equals(targetStatus);
        String journalNo = postingResult == null ? null : postingResult.getJournalNo();
        Instant now = Instant.now();
        if (PspCallbackConstants.BIZ_TYPE_PAY_ORDER.equals(bizType)) {
            if (success) {
                wrapper.set("paid_amount", PspCallbackUtils.defaultAmount(result.getAmount(), order.amount()))
                        .set("paid_at", now)
                        .set(journalNo != null, "ledger_journal_no", journalNo);
            } else {
                wrapper.set("failed_at", now);
            }
            return;
        }
        if (success) {
            wrapper.set("completed_at", now)
                    .set(journalNo != null, "success_journal_no", journalNo);
        } else {
            String failMsg = StringUtils.left(result.getErrorMessage(), 512);
            wrapper.set("failed_at", now)
                    .set(result.getErrorCode() != null, "fail_code", result.getErrorCode())
                    .set(failMsg != null, "fail_msg", failMsg)
                    .set(journalNo != null, "release_journal_no", journalNo);
        }
    }

    private void applyCommon(UpdateWrapper<?> wrapper, String status, PspCallbackResult result, PspCallbackOrder order) {
        String pspOrderNo = StringUtils.defaultIfBlank(result.getPspOrderNo(), order.pspOrderNo());
        wrapper.set("status", status)
                .set("psp_status", status)
                .set(result.getPspStatus() != null, "psp_raw_status", result.getPspStatus())
                .set(pspOrderNo != null, "psp_order_no", pspOrderNo);
    }

    private boolean update(String bizType, Long id, Consumer<UpdateWrapper<?>> setter) {
        return PspCallbackConstants.BIZ_TYPE_PAY_ORDER.equals(bizType)
                ? update(payOrderDao, id, setter)
                : update(payoutOrderDao, id, setter);
    }

    private <T> boolean update(BaseMapper<T> dao, Long id, Consumer<UpdateWrapper<?>> setter) {
        UpdateWrapper<T> wrapper = new UpdateWrapper<>();
        wrapper.eq("id", id)
                .in("status", PspCallbackConstants.STATUS_CREATED, PspCallbackConstants.STATUS_PROCESSING);
        setter.accept(wrapper);
        return dao.update(null, wrapper) > 0;
    }

    private boolean updateJournalNo(String bizType, Long id, Consumer<UpdateWrapper<?>> setter) {
        return PspCallbackConstants.BIZ_TYPE_PAY_ORDER.equals(bizType)
                ? updateJournalNo(payOrderDao, id, setter)
                : updateJournalNo(payoutOrderDao, id, setter);
    }

    private <T> boolean updateJournalNo(BaseMapper<T> dao, Long id, Consumer<UpdateWrapper<?>> setter) {
        UpdateWrapper<T> wrapper = new UpdateWrapper<>();
        wrapper.eq("id", id);
        setter.accept(wrapper);
        return dao.update(null, wrapper) > 0;
    }
}
