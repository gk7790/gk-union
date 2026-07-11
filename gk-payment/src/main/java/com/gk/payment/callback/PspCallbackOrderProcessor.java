package com.gk.payment.callback;

import com.gk.common.enums.BizTypeEnum;
import com.gk.ledger.posting.LedgerPostingResult;
import com.gk.payment.enums.PayinOrderStatusEnum;
import com.gk.payment.enums.PayoutOrderStatusEnum;
import com.gk.payment.state.OrderStateChangeContext;
import com.gk.payment.state.PayinOrderStateService;
import com.gk.payment.state.PayoutOrderStateService;
import com.gk.psp.callback.PspCallbackBizException;
import com.gk.psp.callback.model.PspCallbackOrder;
import com.gk.psp.callback.model.PspCallbackResult;
import com.gk.psp.callback.support.PspCallbackAckMapper;
import com.gk.psp.callback.support.PspCallbackUtils;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PspCallbackOrderProcessor {
    private final PayinOrderStateService payinOrderStateService;
    private final PayoutOrderStateService payoutOrderStateService;

    public boolean process(String bizType, PspCallbackResult result, PspCallbackOrder order, LedgerPostingResult postingResult) {
        String fromStatus = order.status();
        String targetStatus = PspCallbackUtils.normalizeStatus(result.getOrderStatus());
        if (PayinOrderStatusEnum.PROCESSING.code().equals(targetStatus)) {
            if (PspCallbackUtils.isFinalTerminal(fromStatus)
                    || PayinOrderStatusEnum.MANUAL_REVIEW.code().equals(fromStatus)
                    || PayoutOrderStatusEnum.MANUAL_REVIEW.code().equals(fromStatus)) {
                return false;
            }
            return BizTypeEnum.PAYIN_ORDER.matches(bizType)
                    ? payinOrderStateService.applyProcessingResult(order, result, context(bizType, targetStatus, result, order))
                    : payoutOrderStateService.applyProcessingResult(order, result, context(bizType, targetStatus, result, order));
        }
        if (!PspCallbackUtils.isTerminal(targetStatus)) {
            throw new PspCallbackBizException(PspCallbackAckMapper.UNSUPPORTED_STATUS, "Unsupported callback order status");
        }
        if (targetStatus.equals(fromStatus) || PspCallbackUtils.isFinalTerminal(fromStatus)) {
            return false;
        }
        return BizTypeEnum.PAYIN_ORDER.matches(bizType)
                ? payinOrderStateService.applyTerminalResult(order, result, targetStatus, postingResult, context(bizType, targetStatus, result, order))
                : payoutOrderStateService.applyTerminalResult(order, result, targetStatus, postingResult, context(bizType, targetStatus, result, order));
    }

    public void attachPostingResult(String bizType, Long orderId, String targetStatus, LedgerPostingResult postingResult) {
        String journalNo = postingResult == null ? null : postingResult.getJournalNo();
        if (StringUtils.isBlank(journalNo)) {
            return;
        }
        String normalizedStatus = PspCallbackUtils.normalizeStatus(targetStatus);
        boolean updated = BizTypeEnum.PAYIN_ORDER.matches(bizType)
                ? payinOrderStateService.attachLedgerJournal(orderId, postingResult)
                : payoutOrderStateService.attachLedgerJournal(orderId, normalizedStatus, postingResult);
        if (!updated) {
            throw new IllegalStateException("Attach ledger journal to callback order failed");
        }
    }

    private String statusEventType(String bizType, String status) {
        String prefix = BizTypeEnum.PAYIN_ORDER.matches(bizType) ? "PAYIN" : "PAYOUT";
        return prefix + "_" + PspCallbackUtils.normalizeStatus(status);
    }

    private OrderStateChangeContext context(String bizType, String status, PspCallbackResult result, PspCallbackOrder order) {
        return new OrderStateChangeContext(
                statusEventType(bizType, status),
                result == null ? null : result.getErrorMessage(),
                "PSP",
                order == null ? null : order.pspCode(),
                null,
                null
        );
    }
}
