package com.gk.payment.sandbox;

import com.gk.common.constant.Constant;
import com.gk.common.context.ReqContextHolder;
import com.gk.common.enums.BizTypeEnum;
import com.gk.common.enums.PayDirectionEnum;
import com.gk.common.enums.SubjectTypeEnum;
import com.gk.common.exception.ErrorCode;
import com.gk.common.exception.GkException;
import com.gk.common.model.DynMap;
import com.gk.common.utils.BizKeyUtils;
import com.gk.merchant.dao.MerchantAppDao;
import com.gk.merchant.entity.MerchantAppEntity;
import com.gk.merchant.enums.MerchantAppEnvEnum;
import com.gk.payment.callback.PspCallbackNotifyCreator;
import com.gk.payment.callback.PspCallbackOrderProcessor;
import com.gk.payment.callback.PspCallbackOrderResolver;
import com.gk.payment.callback.PspOrderResultHandler;
import com.gk.payment.dao.PayinOrderDao;
import com.gk.payment.dao.PayoutOrderDao;
import com.gk.payment.entity.PayinOrderEntity;
import com.gk.payment.entity.PayoutOrderEntity;
import com.gk.payment.enums.PayinOrderStatusEnum;
import com.gk.payment.enums.PayoutOrderStatusEnum;
import com.gk.payment.notify.MerchantNotifyExecutor;
import com.gk.psp.callback.model.PspCallbackOrder;
import com.gk.psp.callback.model.PspCallbackResult;
import com.gk.psp.callback.support.PspCallbackUtils;
import com.gk.psp.query.PspOrderQueryResult;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SandboxOrderService {
    private static final String SANDBOX_PSP_STATUS_SUCCESS = "SANDBOX_SUCCESS";
    private static final String SANDBOX_PSP_STATUS_FAILED = "SANDBOX_FAILED";
    private static final String SANDBOX_FAILED_MESSAGE = "Sandbox payment failed";
    private static final String OUTCOME_SUCCESS = "SUCCESS";
    private static final String OUTCOME_FAILED = "FAILED";

    private final PayinOrderDao payinOrderDao;
    private final PayoutOrderDao payoutOrderDao;
    private final MerchantAppDao merchantAppDao;
    private final PspOrderResultHandler resultHandler;
    private final PspCallbackOrderResolver orderResolver;
    private final PspCallbackOrderProcessor orderProcessor;
    private final PspCallbackNotifyCreator notifyCreator;
    private final MerchantNotifyExecutor merchantNotifyExecutor;
    private final TransactionTemplate transactionTemplate;

    /**
     * 商户后台沙箱 mock 回调：推进订单终态并同步通知商户 notify_url，完整结果始终返回给前端。
     */
    public SandboxOrderResult mockPayCallback(Long orderId, DynMap params) {
        String outcome = normalizeOutcome(params.getStr("outcome"));
        String failReason = normalizeFailReason(params.getStr("failReason"));
        PayinOrderEntity order = requireSandboxPayinOrderById(orderId);
        assertOrderAccess(order.getTenantId(), order.getMerchantId());
        assertTestApp(order.getMerchantAppId());

        SandboxOrderResult orderResult = transactionTemplate.execute(txStatus -> {
            PayinOrderEntity current = requireSandboxPayinOrderById(orderId);
            if (!PayinOrderStatusEnum.PROCESSING.code().equals(current.getStatus())) {
                if (isTerminalPayStatus(current.getStatus())) {
                    return payResult(current, false, "订单已处于终态，仅重发商户通知");
                }
                throw new GkException(ErrorCode.BAD_REQUEST, "仅处理中订单可 mock 回调");
            }
            String status = OUTCOME_SUCCESS.equals(outcome)
                    ? PayinOrderStatusEnum.SUCCESS.code()
                    : PayinOrderStatusEnum.FAILED.code();
            String errorMessage = OUTCOME_FAILED.equals(outcome)
                    ? StringUtils.defaultIfBlank(failReason, SANDBOX_FAILED_MESSAGE)
                    : null;
            boolean changed = completePay(current, status, errorMessage);
            PayinOrderEntity refreshed = payinOrderDao.selectById(orderId);
            String message = changed
                    ? (OUTCOME_SUCCESS.equals(outcome) ? "沙箱代收回调成功" : "沙箱代收回调失败")
                    : "订单状态未变更";
            return payResult(refreshed, changed, message);
        });

        Map<String, Object> notify = merchantNotifyExecutor.sendOnceByBizOrder(BizTypeEnum.PAYIN_ORDER.code(), orderId);
        if (orderResult == null) {
            throw new GkException(ErrorCode.INTERNAL_SERVER_ERROR, "Sandbox mock callback failed");
        }
        return orderResult.withMock(outcome, OUTCOME_FAILED.equals(outcome) ? failReason : null, notify);
    }

    public SandboxOrderResult mockPayoutCallback(Long orderId, DynMap params) {
        String outcome = normalizeOutcome(params.getStr("outcome"));
        String failReason = normalizeFailReason(params.getStr("failReason"));
        PayoutOrderEntity order = requireSandboxPayoutOrderById(orderId);
        assertOrderAccess(order.getTenantId(), order.getMerchantId());
        assertTestApp(order.getMerchantAppId());

        SandboxOrderResult orderResult = transactionTemplate.execute(txStatus -> {
            PayoutOrderEntity current = requireSandboxPayoutOrderById(orderId);
            if (!PayoutOrderStatusEnum.PROCESSING.code().equals(current.getStatus())) {
                if (isTerminalPayoutStatus(current.getStatus())) {
                    return payoutResult(current, false, "订单已处于终态，仅重发商户通知");
                }
                throw new GkException(ErrorCode.BAD_REQUEST, "仅处理中订单可 mock 回调");
            }
            String status = OUTCOME_SUCCESS.equals(outcome)
                    ? PayoutOrderStatusEnum.SUCCESS.code()
                    : PayoutOrderStatusEnum.FAILED.code();
            String errorMessage = OUTCOME_FAILED.equals(outcome)
                    ? StringUtils.defaultIfBlank(failReason, SANDBOX_FAILED_MESSAGE)
                    : null;
            boolean changed = completeSandboxPayout(current, status, errorMessage);
            PayoutOrderEntity refreshed = payoutOrderDao.selectById(orderId);
            String message = changed
                    ? (OUTCOME_SUCCESS.equals(outcome) ? "沙箱代付回调成功" : "沙箱代付回调失败")
                    : "订单状态未变更";
            return payoutResult(refreshed, changed, message);
        });

        Map<String, Object> notify = merchantNotifyExecutor.sendOnceByBizOrder(BizTypeEnum.PAYOUT_ORDER.code(), orderId);
        if (orderResult == null) {
            throw new GkException(ErrorCode.INTERNAL_SERVER_ERROR, "Sandbox mock callback failed");
        }
        return orderResult.withMock(outcome, OUTCOME_FAILED.equals(outcome) ? failReason : null, notify);
    }

    private boolean completePay(PayinOrderEntity order, String status, String errorMessage) {
        PspOrderQueryResult result = PspOrderQueryResult.builder()
                .success(PspCallbackUtils.STATUS_SUCCESS.equals(status))
                .pspCode(Constant.SANDBOX)
                .bizType(BizTypeEnum.PAYIN_ORDER.code())
                .systemOrderNo(order.getPayinOrderNo())
                .merchantOrderNo(order.getMerchantOrderNo())
                .pspOrderNo(StringUtils.defaultIfBlank(order.getPspOrderNo(), Constant.SANDBOX + "_" + order.getPayinOrderNo()))
                .pspStatus(PspCallbackUtils.STATUS_SUCCESS.equals(status) ? SANDBOX_PSP_STATUS_SUCCESS : SANDBOX_PSP_STATUS_FAILED)
                .orderStatus(status)
                .amount(order.getAmount())
                .currency(order.getCurrency())
                .pspRequestNo(BizKeyUtils.genPspRequestNo())
                .responseStatus(200)
                .responseCode(status)
                .responseMessage(StringUtils.defaultIfBlank(errorMessage, "success"))
                .errorCode(PspCallbackUtils.STATUS_FAILED.equals(status) ? "SANDBOX_FAILED" : null)
                .errorMessage(errorMessage)
                .build();
        return resultHandler.handle(BizTypeEnum.PAYIN_ORDER.code(), orderResolver.fromPayinOrder(order, null), result);
    }

    private boolean completeSandboxPayout(PayoutOrderEntity order, String status, String errorMessage) {
        PspCallbackResult result = new PspCallbackResult();
        result.setPspCode(Constant.SANDBOX);
        result.setBizType(BizTypeEnum.PAYOUT_ORDER.code());
        result.setSystemOrderNo(order.getPayoutOrderNo());
        result.setMerchantOrderNo(order.getMerchantOrderNo());
        result.setPspOrderNo(StringUtils.defaultIfBlank(order.getPspOrderNo(), Constant.SANDBOX + "_" + order.getPayoutOrderNo()));
        result.setPspStatus(PspCallbackUtils.STATUS_SUCCESS.equals(status) ? SANDBOX_PSP_STATUS_SUCCESS : SANDBOX_PSP_STATUS_FAILED);
        result.setOrderStatus(status);
        result.setAmount(order.getAmount());
        result.setCurrency(order.getCurrency());
        result.setCallbackType("SANDBOX");
        result.setErrorCode(PspCallbackUtils.STATUS_FAILED.equals(status) ? "SANDBOX_FAILED" : null);
        result.setErrorMessage(errorMessage);

        PspCallbackOrder callbackOrder = orderResolver.fromPayoutOrder(order, null);
        boolean changed = orderProcessor.process(BizTypeEnum.PAYOUT_ORDER.code(), result, callbackOrder, null);
        if (changed) {
            notifyCreator.create(BizTypeEnum.PAYOUT_ORDER.code(), result, callbackOrder, null);
        }
        return changed;
    }

    private PayinOrderEntity requireSandboxPayinOrderById(Long orderId) {
        PayinOrderEntity order = payinOrderDao.selectById(orderId);
        if (order == null) {
            throw new GkException(ErrorCode.NOT_FOUND, "Sandbox pay order not found");
        }
        if (!Constant.SANDBOX.equalsIgnoreCase(order.getPspCode())) {
            throw new GkException(ErrorCode.BAD_REQUEST, "Pay order is not a sandbox order");
        }
        return order;
    }

    private PayoutOrderEntity requireSandboxPayoutOrderById(Long orderId) {
        PayoutOrderEntity order = payoutOrderDao.selectById(orderId);
        if (order == null) {
            throw new GkException(ErrorCode.NOT_FOUND, "Sandbox payout order not found");
        }
        if (!Constant.SANDBOX.equalsIgnoreCase(order.getPspCode())) {
            throw new GkException(ErrorCode.BAD_REQUEST, "Payout order is not a sandbox order");
        }
        return order;
    }

    private void assertOrderAccess(Long tenantId, Long merchantId) {
        if (SubjectTypeEnum.MERCHANT.matches(ReqContextHolder.getSubjectType())) {
            if (ReqContextHolder.getTenantId() == null || ReqContextHolder.getMerchantId() == null) {
                throw new GkException(ErrorCode.DATA_SCOPE_PARAMS_ERROR);
            }
            if (!ReqContextHolder.getTenantId().equals(tenantId) || !ReqContextHolder.getMerchantId().equals(merchantId)) {
                throw new GkException(ErrorCode.FORBIDDEN);
            }
            return;
        }
        if (SubjectTypeEnum.TENANT.matches(ReqContextHolder.getSubjectType())) {
            if (ReqContextHolder.getTenantId() == null || !ReqContextHolder.getTenantId().equals(tenantId)) {
                throw new GkException(ErrorCode.FORBIDDEN);
            }
        }
    }

    private void assertTestApp(Long merchantAppId) {
        if (merchantAppId == null) {
            throw new GkException(ErrorCode.BAD_REQUEST, "Sandbox order missing merchant app");
        }
        MerchantAppEntity app = merchantAppDao.selectById(merchantAppId);
        if (app == null || !MerchantAppEnvEnum.TEST.code().equals(app.getAppEnv())) {
            throw new GkException(ErrorCode.BAD_REQUEST, "仅测试应用订单可 mock 回调");
        }
    }

    private String normalizeOutcome(String outcome) {
        if (StringUtils.isBlank(outcome)) {
            throw new GkException(ErrorCode.BAD_REQUEST, "outcome is required");
        }
        String normalized = outcome.trim().toUpperCase(Locale.ROOT);
        if (!OUTCOME_SUCCESS.equals(normalized) && !OUTCOME_FAILED.equals(normalized)) {
            throw new GkException(ErrorCode.BAD_REQUEST, "outcome must be SUCCESS or FAILED");
        }
        return normalized;
    }

    private String normalizeFailReason(String failReason) {
        return StringUtils.isBlank(failReason) ? null : StringUtils.abbreviate(failReason.trim(), 500);
    }

    private boolean isTerminalPayStatus(String status) {
        return PayinOrderStatusEnum.SUCCESS.matches(status)
                || PayinOrderStatusEnum.FAILED.matches(status)
                || PayinOrderStatusEnum.CLOSED.matches(status);
    }

    private boolean isTerminalPayoutStatus(String status) {
        return PayoutOrderStatusEnum.SUCCESS.matches(status)
                || PayoutOrderStatusEnum.FAILED.matches(status)
                || PayoutOrderStatusEnum.CANCELLED.matches(status);
    }

    private SandboxOrderResult payResult(PayinOrderEntity order, boolean changed, String message) {
        return SandboxOrderResult.basic(
                PayDirectionEnum.PAYIN.code(),
                order.getPayinOrderNo(),
                order.getMerchantOrderNo(),
                order.getStatus(),
                changed,
                message
        );
    }

    private SandboxOrderResult payoutResult(PayoutOrderEntity order, boolean changed, String message) {
        return SandboxOrderResult.basic(
                PayDirectionEnum.PAYOUT.code(),
                order.getPayoutOrderNo(),
                order.getMerchantOrderNo(),
                order.getStatus(),
                changed,
                message
        );
    }
}
