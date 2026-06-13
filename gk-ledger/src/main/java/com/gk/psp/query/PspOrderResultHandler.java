package com.gk.psp.query;

import com.gk.common.enums.BizTypeEnum;
import com.gk.ledger.posting.LedgerPostingResult;
import com.gk.ledger.posting.PaySuccessPostingRequest;
import com.gk.ledger.posting.PayoutPostingRequest;
import com.gk.ledger.service.LedgerPostingService;
import com.gk.payment.enums.PayOrderStatusEnum;
import com.gk.payment.enums.PayoutOrderStatusEnum;
import com.gk.payment.service.PayOrderService;
import com.gk.psp.callback.model.PspCallbackOrder;
import com.gk.psp.callback.model.PspCallbackResult;
import com.gk.psp.callback.support.PspCallbackNotifyCreator;
import com.gk.psp.callback.support.PspCallbackOrderProcessor;
import com.gk.psp.callback.support.PspCallbackUtils;
import com.gk.psp.callback.support.PspCallbackValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PspOrderResultHandler {
    private final PspCallbackOrderProcessor orderProcessor;
    private final PspCallbackValidator callbackValidator;
    private final LedgerPostingService ledgerPostingService;
    private final PspCallbackNotifyCreator notifyCreator;
    private final PayOrderService payOrderService;

    @Transactional(rollbackFor = Exception.class)
    public boolean handle(String bizType, PspCallbackOrder order, PspOrderQueryResult queryResult) {
        PspCallbackResult result = queryResult.toCallbackResult(bizType);
        boolean terminal = PspCallbackUtils.isTerminal(result.getOrderStatus());
        if (terminal) {
            callbackValidator.validateTerminalCallback(bizType, result, order);
        }
        boolean orderChanged = orderProcessor.process(bizType, result, order, null);
        if (orderChanged && terminal) {
            LedgerPostingResult postingResult = postLedger(bizType, result, order);
            orderProcessor.attachPostingResult(bizType, order.id(), result.getOrderStatus(), postingResult);
            if (BizTypeEnum.PAY_ORDER.matches(bizType)
                    && PayOrderStatusEnum.SUCCESS.code().equals(PspCallbackUtils.normalizeStatus(result.getOrderStatus()))) {
                payOrderService.onPaySuccessPosted(order.id());
            }
            notifyCreator.create(bizType, result, order, null);
        }
        return orderChanged;
    }

    private LedgerPostingResult postLedger(String bizType, PspCallbackResult result, PspCallbackOrder order) {
        String status = PspCallbackUtils.normalizeStatus(result.getOrderStatus());
        if (BizTypeEnum.PAY_ORDER.matches(bizType) && PayOrderStatusEnum.SUCCESS.code().equals(status)) {
            return ledgerPostingService.postPaySuccess(paySuccessRequest(result, order));
        }
        if (BizTypeEnum.PAYOUT_ORDER.matches(bizType) && PayoutOrderStatusEnum.SUCCESS.code().equals(status)) {
            return ledgerPostingService.postPayoutSuccess(payoutRequest(order));
        }
        if (BizTypeEnum.PAYOUT_ORDER.matches(bizType) && PayoutOrderStatusEnum.FAILED.code().equals(status)) {
            return ledgerPostingService.releasePayout(payoutRequest(order));
        }
        return null;
    }

    private PaySuccessPostingRequest paySuccessRequest(PspCallbackResult result, PspCallbackOrder order) {
        PaySuccessPostingRequest request = new PaySuccessPostingRequest();
        request.setTenantId(order.tenantId());
        request.setMerchantId(order.merchantId());
        request.setBizId(order.id());
        request.setPayOrderNo(order.orderNo());
        request.setCurrency(order.currency());
        request.setAmount(PspCallbackUtils.defaultAmount(result.getAmount(), order.amount()));
        request.setMerchantFeeAmount(order.merchantFeeAmount());
        request.setSettleAmount(order.settleAmount());
        return request;
    }

    private PayoutPostingRequest payoutRequest(PspCallbackOrder order) {
        PayoutPostingRequest request = new PayoutPostingRequest();
        request.setTenantId(order.tenantId());
        request.setMerchantId(order.merchantId());
        request.setBizId(order.id());
        request.setPayoutOrderNo(order.orderNo());
        request.setCurrency(order.currency());
        request.setAmount(order.amount());
        request.setMerchantFeeAmount(order.merchantFeeAmount());
        request.setTotalDebitAmount(order.totalDebitAmount());
        return request;
    }
}
