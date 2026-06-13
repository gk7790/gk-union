package com.gk.psp.callback;

import com.gk.common.enums.BizTypeEnum;
import com.gk.ledger.posting.PaySuccessPostingRequest;
import com.gk.ledger.posting.LedgerPostingResult;
import com.gk.ledger.posting.PayoutPostingRequest;
import com.gk.ledger.service.LedgerPostingService;
import com.gk.payment.enums.PayOrderStatusEnum;
import com.gk.payment.service.PayOrderService;
import com.gk.psp.callback.adapter.PspCallbackAdapter;
import com.gk.psp.enums.PspCallbackProcessStatusEnum;
import com.gk.psp.enums.PspCallbackVerifyStatusEnum;
import com.gk.psp.callback.model.PspCallbackOrder;
import com.gk.psp.callback.model.PspCallbackRequest;
import com.gk.psp.callback.model.PspCallbackResult;
import com.gk.psp.callback.support.*;
import com.gk.psp.entity.PspCallbackLogEntity;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.transaction.support.TransactionSynchronizationManager;

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
    private final PspCallbackValidator callbackValidator;
    private final PayOrderService payOrderService;

    @Transactional(rollbackFor = Exception.class)
    public String handlePayCallback(String pspCode, HttpServletRequest request, String rawBody) {
        return handle(pspCode, BizTypeEnum.PAY_ORDER.code(), request, rawBody);
    }

    @Transactional(rollbackFor = Exception.class)
    public String handlePayoutCallback(String pspCode, HttpServletRequest request, String rawBody) {
        return handle(pspCode, BizTypeEnum.PAYOUT_ORDER.code(), request, rawBody);
    }

    private String handle(String pspCode, String bizType, HttpServletRequest servletRequest, String rawBody) {
        PspCallbackAdapter adapter = findAdapter(pspCode);
        if (adapter == null) {
            return "fail";
        }

        PspCallbackRequest request = requestFactory.create(pspCode, bizType, servletRequest, rawBody);
        PspCallbackLogEntity logEntity = null;
        boolean orderChanged = false;
        try {
            PspCallbackResult result = parse(adapter, bizType, request);
            PspCallbackOrder order = orderResolver.resolve(bizType, result);
            request.setApiSecret(order.apiSecret());
            logEntity = logRecorder.received(request, result, order);

            if (!adapter.verifySign(request)) {
                logRecorder.finish(logEntity, PspCallbackVerifyStatusEnum.FAILED.code(), PspCallbackProcessStatusEnum.FAILED.code(), "Invalid PSP callback signature");
                return result.getFailResponse();
            }

            boolean terminal = PspCallbackUtils.isTerminal(result.getOrderStatus());
            if (terminal) {
                callbackValidator.validateTerminalCallback(bizType, result, order);
            }
            orderChanged = orderProcessor.process(bizType, result, order, null);
            if (orderChanged && terminal) {
                LedgerPostingResult postingResult = postLedger(bizType, result, order);
                orderProcessor.attachPostingResult(bizType, order.id(), result.getOrderStatus(), postingResult);
                if (BizTypeEnum.PAY_ORDER.matches(bizType)
                        && PayOrderStatusEnum.SUCCESS.code().equals(PspCallbackUtils.normalizeStatus(result.getOrderStatus()))) {
                    payOrderService.onPaySuccessPosted(order.id());
                }
                notifyCreator.create(bizType, result, order, logEntity);
            }

            logRecorder.finish(logEntity, PspCallbackVerifyStatusEnum.SUCCESS.code(), orderChanged ? PspCallbackProcessStatusEnum.SUCCESS.code() : PspCallbackProcessStatusEnum.IGNORED.code(), null);
            return result.getSuccessResponse();
        } catch (Exception ex) {
            if (orderChanged) {
                rollbackIfActive();
            }
            if (logEntity == null) {
                logEntity = logRecorder.failed(pspCode, bizType, request, ex);
            }
            logRecorder.finish(logEntity, PspCallbackVerifyStatusEnum.FAILED.code(), PspCallbackProcessStatusEnum.FAILED.code(), ex.getMessage());
            return "fail";
        }
    }

    private void rollbackIfActive() {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        }
    }

    private PspCallbackResult parse(PspCallbackAdapter adapter, String bizType, PspCallbackRequest request) {
        if (BizTypeEnum.PAY_ORDER.matches(bizType)) {
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
        if (BizTypeEnum.PAY_ORDER.matches(bizType) && PayOrderStatusEnum.SUCCESS.code().equals(status)) {
            return ledgerPostingService.postPaySuccess(paySuccessRequest(result, order));
        }
        if (BizTypeEnum.PAYOUT_ORDER.matches(bizType) && PayOrderStatusEnum.SUCCESS.code().equals(status)) {
            return ledgerPostingService.postPayoutSuccess(payoutRequest(order));
        }
        if (BizTypeEnum.PAYOUT_ORDER.matches(bizType) && PayOrderStatusEnum.FAILED.code().equals(status)) {
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
