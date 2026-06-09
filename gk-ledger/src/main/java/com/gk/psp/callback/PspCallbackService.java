package com.gk.psp.callback;

import com.gk.ledger.posting.PaySuccessPostingRequest;
import com.gk.ledger.posting.LedgerPostingResult;
import com.gk.ledger.posting.PayoutPostingRequest;
import com.gk.ledger.service.LedgerPostingService;
import com.gk.psp.callback.adapter.PspCallbackAdapter;
import com.gk.psp.callback.model.PspCallbackOrder;
import com.gk.psp.callback.model.PspCallbackRequest;
import com.gk.psp.callback.model.PspCallbackResult;
import com.gk.psp.callback.support.*;
import com.gk.psp.entity.PspCallbackLogEntity;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 串联整个流程，是唯一的协调中心
 */
@Service
@RequiredArgsConstructor
public class PspCallbackService {
    private final List<PspCallbackAdapter> adapters;
    private final PspCallbackRequestFactory requestFactory;
    private final PspCallbackOrderResolver orderResolver;
    private final PspCallbackOrderProcessor orderProcessor;
    private final PspCallbackLogRecorder logRecorder;
    private final PspCallbackNotifyCreator notifyCreator;
    private final LedgerPostingService ledgerPostingService;

    public String handlePayCallback(String pspCode, HttpServletRequest request, String rawBody) {
        return handle(pspCode, PspCallbackConstants.BIZ_TYPE_PAY_ORDER, request, rawBody);
    }

    public String handlePayoutCallback(String pspCode, HttpServletRequest request, String rawBody) {
        return handle(pspCode, PspCallbackConstants.BIZ_TYPE_PAYOUT_ORDER, request, rawBody);
    }

    private String handle(String pspCode, String bizType, HttpServletRequest servletRequest, String rawBody) {
        PspCallbackAdapter adapter = findAdapter(pspCode);
        if (adapter == null) {
            return "fail";
        }

        PspCallbackRequest request = requestFactory.create(pspCode, bizType, servletRequest, rawBody);
        PspCallbackLogEntity logEntity = null;
        try {
            PspCallbackResult result = parse(adapter, bizType, request);
            PspCallbackOrder order = orderResolver.resolve(bizType, result);
            request.setApiSecret(order.apiSecret());
            logEntity = logRecorder.received(request, result, order);

            if (!adapter.verifySign(request)) {
                logRecorder.finish(logEntity, "FAILED", PspCallbackConstants.PROCESS_FAILED, "Invalid PSP callback signature");
                return result.getFailResponse();
            }

            LedgerPostingResult postingResult = null;
            if (PspCallbackUtils.isTerminal(result.getOrderStatus())) {
                postingResult = postLedger(bizType, result, order);
            }
            boolean changed = orderProcessor.process(bizType, result, order, postingResult);
            if (changed && PspCallbackUtils.isTerminal(result.getOrderStatus())) {
                notifyCreator.create(bizType, result, order, logEntity);
            }

            logRecorder.finish(logEntity, "SUCCESS", changed ? PspCallbackConstants.PROCESS_SUCCESS : PspCallbackConstants.PROCESS_IGNORED, null);
            return result.getSuccessResponse();
        } catch (Exception ex) {
            if (logEntity == null) {
                logEntity = logRecorder.failed(pspCode, bizType, request, ex);
            }
            logRecorder.finish(logEntity, "FAILED", PspCallbackConstants.PROCESS_FAILED, ex.getMessage());
            return "fail";
        }
    }

    private PspCallbackResult parse(PspCallbackAdapter adapter, String bizType, PspCallbackRequest request) {
        if (PspCallbackConstants.BIZ_TYPE_PAY_ORDER.equals(bizType)) {
            return adapter.parsePayCallback(request);
        }
        return adapter.parsePayoutCallback(request);
    }

    private PspCallbackAdapter findAdapter(String pspCode) {
        return adapters.stream()
                .filter(adapter -> adapter.supports(pspCode))
                .findFirst()
                .orElse(null);
    }

    private LedgerPostingResult postLedger(String bizType, PspCallbackResult result, PspCallbackOrder order) {
        String status = PspCallbackUtils.normalizeStatus(result.getOrderStatus());
        if (PspCallbackConstants.BIZ_TYPE_PAY_ORDER.equals(bizType) && PspCallbackConstants.STATUS_SUCCESS.equals(status)) {
            return ledgerPostingService.postPaySuccess(paySuccessRequest(result, order));
        }
        if (PspCallbackConstants.BIZ_TYPE_PAYOUT_ORDER.equals(bizType) && PspCallbackConstants.STATUS_SUCCESS.equals(status)) {
            return ledgerPostingService.postPayoutSuccess(payoutRequest(order));
        }
        if (PspCallbackConstants.BIZ_TYPE_PAYOUT_ORDER.equals(bizType) && PspCallbackConstants.STATUS_FAILED.equals(status)) {
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
