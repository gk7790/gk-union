package com.gk.psp.callback;

import com.gk.common.enums.BizTypeEnum;
import com.gk.common.model.Result;
import com.gk.infra.ipwhitelist.service.PspCallbackIpWhitelistService;
import com.gk.ledger.posting.LedgerPostingResult;
import com.gk.ledger.posting.PaySuccessPostingRequest;
import com.gk.ledger.posting.PayoutPostingRequest;
import com.gk.ledger.service.LedgerPostingService;
import com.gk.payment.enums.PayOrderStatusEnum;
import com.gk.payment.service.PayOrderService;
import com.gk.psp.callback.adapter.PspCallbackAdapter;
import com.gk.psp.callback.model.PspCallbackContext;
import com.gk.psp.callback.model.PspCallbackHandleResult;
import com.gk.psp.callback.model.PspCallbackOrder;
import com.gk.psp.callback.model.PspCallbackRequest;
import com.gk.psp.callback.model.PspCallbackResponse;
import com.gk.psp.callback.model.PspCallbackResult;
import com.gk.psp.callback.support.PspCallbackAckMapper;
import com.gk.psp.callback.support.PspCallbackLogRecorder;
import com.gk.psp.callback.support.PspCallbackNotifyCreator;
import com.gk.psp.callback.support.PspCallbackOrderProcessor;
import com.gk.psp.callback.support.PspCallbackOrderResolver;
import com.gk.psp.callback.support.PspCallbackRequestFactory;
import com.gk.psp.callback.support.PspCallbackUtils;
import com.gk.psp.callback.support.PspCallbackValidator;
import com.gk.psp.enums.PspCallbackProcessStatusEnum;
import com.gk.psp.enums.PspCallbackVerifyStatusEnum;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

/**
 * PSP 回调处理编排服务。
 * <p>
 * 本类不负责某个 PSP 的报文解析或订单 SQL 细节，只负责把回调主链路串起来：
 * <ol>
 *   <li>组装统一请求、IP 白名单校验、选择适配器并解析报文</li>
 *   <li>定位平台订单、记录回调日志、验签与终态字段校验</li>
 *   <li>更新订单状态、执行账务入账/解冻、创建商户通知任务</li>
 *   <li>按 PSP 协议映射 HTTP 响应（成功返回 PSP 约定文本，失败返回结构化 Result）</li>
 * </ol>
 * <p>
 * 代收与代付共用 {@link #handle} 主流程，通过 {@code bizType} 区分；要求回调日志可追溯、
 * 重复回调幂等、终态才触发账务，且订单状态与账务凭证保持一致。
 */
@Service
@RequiredArgsConstructor
@Slf4j
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
    private final PspCallbackIpWhitelistService pspCallbackIpWhitelistService;
    private final PspCallbackAckMapper ackMapper;

    /**
     * 处理 PSP 代收回调。
     *
     * @param pspCode PSP 编码，用于选择对应回调适配器
     * @param request HTTP 原始请求
     * @param rawBody   原始请求体，供验签、日志及 JSON 解析使用
     * @return 含 HTTP 状态与响应体的回调应答，成功时 body 为 PSP 约定成功文本
     */
    @Transactional(rollbackFor = Exception.class)
    public PspCallbackResponse handlePayCallback(String pspCode, HttpServletRequest request, String rawBody) {
        return handle(pspCode, BizTypeEnum.PAY_ORDER.code(), request, rawBody);
    }

    /**
     * 处理 PSP 代付回调。
     *
     * @param pspCode PSP 编码，用于选择对应回调适配器
     * @param request HTTP 原始请求
     * @param rawBody   原始请求体，供验签、日志及 JSON 解析使用
     * @return 含 HTTP 状态与响应体的回调应答，成功时 body 为 PSP 约定成功文本
     */
    @Transactional(rollbackFor = Exception.class)
    public PspCallbackResponse handlePayoutCallback(String pspCode, HttpServletRequest request, String rawBody) {
        return handle(pspCode, BizTypeEnum.PAYOUT_ORDER.code(), request, rawBody);
    }

    /**
     * PSP 回调统一处理主流程。
     * <p>
     * 分三阶段推进：{@link #prepare} → {@link #verifyAndValidate} → {@link #process}。
     * 任一阶段失败则写失败日志并按 PSP 协议返回失败应答；订单已变更但后续失败时会回滚事务。
     */
    private PspCallbackResponse handle(String pspCode, String bizType, HttpServletRequest servletRequest, String rawBody) {
        PspCallbackRequest request = requestFactory.create(pspCode, bizType, servletRequest, rawBody);
        PspCallbackContext context = PspCallbackContext.of(pspCode, bizType, request);
        // 默认失败应答；解析成功后替换为适配器配置的 failResponse。
        String failResponse = PspCallbackResponse.DEFAULT_FAIL_BODY;
        try {
            // 数据准备阶段：IP 白名单、适配器解析、订单定位、回调日志上下文组装。
            Result<PspCallbackContext> prepared = prepare(context);
            if (prepared.isFail()) {
                return finishFailed(context, prepared, failResponse);
            }

            context = prepared.getData();
            failResponse = failBody(context.getResult());

            // 校验阶段：验签、判断是否终态、终态时校验金额/币种/PSP编码等关键字段。
            Result<PspCallbackContext> verified = verifyAndValidate(context);
            if (verified.isFail()) {
                return finishFailed(context, verified, failResponse);
            }

            // 处理阶段：更新订单、终态入账/解冻、补写流水号、创建商户通知。
            Result<PspCallbackHandleResult> handled = process(context);
            if (handled.isFail()) {
                return finishFailed(context, handled, failResponse);
            }

            PspCallbackHandleResult result = handled.getData();
            logRecorder.finish(
                    context.getLog(),
                    result.getVerifyStatus(),
                    result.getProcessStatus(),
                    null
            );
            return ackMapper.toResponse(handled, failResponse);
        } catch (Exception ex) {
            if (context.isOrderChanged()) {
                // 订单已更新但账务/通知失败时，标记事务回滚，避免订单终态与账务不一致。
                rollbackIfActive();
            }
            if (context.getLog() == null) {
                context.setLog(logRecorder.failed(pspCode, bizType, request, ex));
            }
            logRecorder.finish(
                    context.getLog(),
                    PspCallbackVerifyStatusEnum.FAILED.code(),
                    PspCallbackProcessStatusEnum.FAILED.code(),
                    ex.getMessage()
            );
            PspCallbackResponse response = ackMapper.toResponse(Result.fail(PspCallbackAckMapper.SYSTEM_ERROR), failResponse);
            logFailure(pspCode, bizType, response.status(), ex);
            return response;
        }
    }

    /**
     * 准备阶段：IP 白名单、适配器解析、订单定位、回调日志上下文组装。
     */
    private Result<PspCallbackContext> prepare(PspCallbackContext context) {
        PspCallbackRequest request = context.getRequest();
        if (!pspCallbackIpWhitelistService.isPspCallbackAllowed(request.getPspCode(), request.getClientIp())) {
            return Result.fail(PspCallbackAckMapper.IP_FORBIDDEN);
        }

        PspCallbackAdapter adapter = findAdapter(request.getPspCode());
        if (adapter == null) {
            return Result.fail(PspCallbackAckMapper.ADAPTER_NOT_FOUND);
        }

        // 适配器将 PSP 原始报文解析为平台统一的 PspCallbackResult（含 orderStatus 映射）。
        Result<PspCallbackResult> parsed = BizTypeEnum.PAY_ORDER.matches(context.getBizType())
                ? adapter.parsePayCallback(request)
                : adapter.parsePayoutCallback(request);

        if (parsed.isFail()) {
            return Result.fail(parsed.getMsg());
        }
        context.setResult(parsed.getData());

        try {
            // 解析 PSP 回调对应的平台订单。
            PspCallbackOrder order = orderResolver.resolve(context.getBizType(), context.getResult());
            context.setOrder(order);
            request.setApiSecret(order.apiSecret());
            // 先组装回调日志上下文，验签/处理结果在 finish 时异步落库。
            context.setLog(logRecorder.received(request, context.getResult(), order));
            return Result.success(context);
        } catch (IllegalStateException ex) {
            return Result.fail(orderResolveCode(ex));
        }
    }

    /**
     * 校验阶段：验签、判断是否终态、终态时校验金额/币种/PSP 编码等关键字段。
     */
    private Result<PspCallbackContext> verifyAndValidate(PspCallbackContext context) {
        PspCallbackAdapter adapter = findAdapter(context.getRequest().getPspCode());
        if (adapter == null) {
            return Result.fail(PspCallbackAckMapper.ADAPTER_NOT_FOUND);
        }

        Result<Void> signed = adapter.verifySign(context.getRequest(), context.getOrder());
        if (signed.isFail()) {
            return Result.fail(StringUtils.defaultIfBlank(signed.getMsg(), PspCallbackAckMapper.SIGN_INVALID));
        }

        // orderStatus 已由适配器映射为平台枚举；此处判断是否为 SUCCESS/FAILED/CLOSED 等终态。
        context.setTerminal(PspCallbackUtils.isTerminal(context.getResult().getOrderStatus()));
        if (!context.isTerminal()) {
            // 中间态（如 PROCESSING）只更新订单进度，不做终态金额校验。
            return Result.success(context);
        }

        Result<Void> validated = callbackValidator.validateTerminalCallbackResult(
                context.getBizType(),
                context.getResult(),
                context.getOrder()
        );
        if (validated.isFail()) {
            return Result.fail(validated.getMsg());
        }
        return Result.success(context);
    }

    /**
     * 处理阶段：更新订单、终态入账/解冻、补写流水号、创建商户通知。
     * <p>
     * 仅当 {@code orderChanged && terminal} 时才执行账务与通知，避免重复回调或中间态误入账。
     */
    private Result<PspCallbackHandleResult> process(PspCallbackContext context) {
        try {
            boolean orderChanged = orderProcessor.process(context.getBizType(), context.getResult(), context.getOrder(), null);
            context.setOrderChanged(orderChanged);
            if (orderChanged && context.isTerminal()) {
                LedgerPostingResult postingResult = postLedger(context.getBizType(), context.getResult(), context.getOrder());
                orderProcessor.attachPostingResult(
                        context.getBizType(),
                        context.getOrder().id(),
                        context.getResult().getOrderStatus(),
                        postingResult
                );
                if (BizTypeEnum.PAY_ORDER.matches(context.getBizType())
                        && PayOrderStatusEnum.SUCCESS.code().equals(PspCallbackUtils.normalizeStatus(context.getResult().getOrderStatus()))) {
                    // 代收成功入账后计算待结算释放时间。
                    payOrderService.onPaySuccessPosted(context.getOrder().id());
                }
                notifyCreator.create(context.getBizType(), context.getResult(), context.getOrder(), context.getLog());
            }
            // 重复回调或未发生状态变更记为 IGNORED，仍向 PSP 返回成功应答。
            String processStatus = orderChanged
                    ? PspCallbackProcessStatusEnum.SUCCESS.code()
                    : PspCallbackProcessStatusEnum.IGNORED.code();
            PspCallbackHandleResult result = PspCallbackHandleResult.of(
                    PspCallbackResponse.ok(context.getResult().getSuccessResponse()),
                    PspCallbackVerifyStatusEnum.SUCCESS.code(),
                    processStatus,
                    orderChanged,
                    context.isTerminal()
            );
            return Result.success(result);
        } catch (IllegalStateException ex) {
            if (StringUtils.containsIgnoreCase(ex.getMessage(), "Unsupported callback order status")) {
                return Result.fail(PspCallbackAckMapper.UNSUPPORTED_STATUS);
            }
            throw ex;
        }
    }

    /**
     * 阶段失败统一收尾：补写失败日志，并按错误码映射 HTTP 应答。
     */
    private PspCallbackResponse finishFailed(PspCallbackContext context, Result<?> failure, String failResponse) {
        if (context.getLog() == null) {
            context.setLog(logRecorder.failed(context.getPspCode(), context.getBizType(), context.getRequest(),
                    new IllegalStateException(failureMessage(failure))));
        }
        String code = failureMessage(failure);
        // 验签已通过但业务校验失败（如金额不符）时，日志记 verify=SUCCESS 便于区分攻击与业务拒绝。
        String verifyStatus = verifyStatusForFailure(code);
        logRecorder.finish(
                context.getLog(),
                verifyStatus,
                PspCallbackProcessStatusEnum.FAILED.code(),
                code
        );
        PspCallbackResponse response = ackMapper.toResponse(Result.fail(code), failResponse);
        logFailure(context.getPspCode(), context.getBizType(), response.status(), new IllegalStateException(code));
        return response;
    }

    private String failureMessage(Result<?> failure) {
        return failure == null ? PspCallbackAckMapper.SYSTEM_ERROR
                : StringUtils.defaultIfBlank(failure.getMsg(), PspCallbackAckMapper.SYSTEM_ERROR);
    }

    /**
     * 根据失败错误码决定回调日志中的验签状态。
     * <p>
     * 金额/币种/PSP 编码不匹配等属于「验签后业务拒绝」，与签名无效区分开。
     */
    private String verifyStatusForFailure(String code) {
        if (StringUtils.equalsAny(code,
                PspCallbackAckMapper.AMOUNT_MISMATCH,
                PspCallbackAckMapper.CURRENCY_MISMATCH,
                PspCallbackAckMapper.PSP_CODE_MISMATCH,
                PspCallbackAckMapper.UNSUPPORTED_STATUS)) {
            return PspCallbackVerifyStatusEnum.SUCCESS.code();
        }
        return PspCallbackVerifyStatusEnum.FAILED.code();
    }

    private String orderResolveCode(Exception ex) {
        if (StringUtils.containsIgnoreCase(ex.getMessage(), "not found")) {
            return PspCallbackAckMapper.ORDER_NOT_FOUND;
        }
        return PspCallbackAckMapper.SYSTEM_ERROR;
    }

    private String failBody(PspCallbackResult result) {
        if (result == null) {
            return PspCallbackResponse.DEFAULT_FAIL_BODY;
        }
        return StringUtils.defaultIfBlank(result.getFailResponse(), PspCallbackResponse.DEFAULT_FAIL_BODY);
    }

    /** 按 PSP 编码选择支持该 PSP 的回调适配器。 */
    private PspCallbackAdapter findAdapter(String pspCode) {
        return adapters.stream()
                .filter(adapter -> adapter.supports(pspCode))
                .findFirst()
                .orElse(null);
    }

    /**
     * 根据回调终态执行账务处理。
     * <p>
     * 代收成功：清算户 → 商户待结算 + 内部手续费收入；代付成功：消费冻结；代付失败：释放冻结回可用。
     */
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

    /** 构建代收成功入账请求；回调金额为空时回退到订单金额。 */
    private PaySuccessPostingRequest paySuccessRequest(PspCallbackResult result, PspCallbackOrder order) {
        PaySuccessPostingRequest request = new PaySuccessPostingRequest();
        request.setTenantId(order.tenantId());
        request.setMerchantId(order.merchantId());
        request.setPspAccountId(order.pspAccountId());
        request.setBizId(order.id());
        request.setPayOrderNo(order.orderNo());
        request.setCurrency(order.currency());
        request.setAmount(PspCallbackUtils.defaultAmount(result.getAmount(), order.amount()));
        request.setMerchantFeeAmount(order.merchantFeeAmount());
        request.setSettleAmount(order.settleAmount());
        return request;
    }

    /** 构建代付账务请求；成功消费冻结与失败解冻共用同一批订单金额信息。 */
    private PayoutPostingRequest payoutRequest(PspCallbackOrder order) {
        PayoutPostingRequest request = new PayoutPostingRequest();
        request.setTenantId(order.tenantId());
        request.setMerchantId(order.merchantId());
        request.setPspAccountId(order.pspAccountId());
        request.setBizId(order.id());
        request.setPayoutOrderNo(order.orderNo());
        request.setCurrency(order.currency());
        request.setAmount(order.amount());
        request.setMerchantFeeAmount(order.merchantFeeAmount());
        request.setTotalDebitAmount(order.totalDebitAmount());
        return request;
    }

    /** 将当前 Spring 事务标记为仅回滚，用于订单已更新但后续步骤失败的补偿。 */
    private void rollbackIfActive() {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        }
    }

    private void logFailure(String pspCode, String bizType, HttpStatus status, Exception ex) {
        if (status.is5xxServerError()) {
            log.error("PSP callback failed, pspCode={}, bizType={}, status={}", pspCode, bizType, status.value(), ex);
            return;
        }
        log.warn("PSP callback rejected, pspCode={}, bizType={}, status={}, reason={}",
                pspCode, bizType, status.value(), ex.getMessage());
    }
}
