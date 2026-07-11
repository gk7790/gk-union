package com.gk.payment.callback;

import com.gk.common.enums.BizTypeEnum;
import com.gk.common.model.Result;
import com.gk.common.redis.PaymentRedisKeys;
import com.gk.common.redis.RedisKeys;
import com.gk.common.redis.RedisUtils;
import com.gk.infra.ipwhitelist.service.PspCallbackIpWhitelistService;
import com.gk.ledger.posting.LedgerPostingResult;
import com.gk.ledger.posting.PaySuccessPostingRequest;
import com.gk.ledger.posting.PayoutPostingRequest;
import com.gk.ledger.service.LedgerPostingService;
import com.gk.payment.enums.PayinOrderStatusEnum;
import com.gk.payment.service.PayinOrderService;
import com.gk.psp.callback.PspCallbackBizException;
import com.gk.psp.callback.PspCallbackException;
import com.gk.psp.adapter.PspCallbackAdapter;
import com.gk.psp.callback.model.PspCallbackAccount;
import com.gk.psp.callback.model.PspCallbackContext;
import com.gk.psp.callback.model.PspCallbackHandleResult;
import com.gk.psp.callback.model.PspCallbackOrder;
import com.gk.psp.callback.model.PspCallbackRequest;
import com.gk.psp.callback.model.PspCallbackResponse;
import com.gk.psp.callback.model.PspCallbackResult;
import com.gk.psp.callback.support.PspCallbackAckMapper;
import com.gk.psp.callback.support.PspCallbackLogRecorder;
import com.gk.psp.callback.support.PspCallbackRequestFactory;
import com.gk.psp.callback.support.PspCallbackStatus;
import com.gk.psp.callback.support.PspCallbackUtils;
import com.gk.psp.callback.support.PspCallbackValidator;
import com.gk.psp.dao.PspAccountDao;
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
 * PSP 回调统一编排服务。
 *
 * <p>企业级回调主链路必须先确认来源，再信任报文内容，最后才允许推进订单和账务：
 * <ol>
 *     <li>根据回调路径中的 pspAccountNo 定位 PSP 账号、pspCode 和验签密钥。</li>
 *     <li>按 pspCode 校验 PSP 回调 IP 白名单。</li>
 *     <li>选择 PSP 回调适配器，并把原始报文解析成平台统一结果。</li>
 *     <li>使用账号密钥验签；验签失败时不查询订单、不更新业务数据。</li>
 *     <li>验签通过后再定位平台订单，并校验金额、币种、PSP 编码和终态合法性。</li>
 *     <li>幂等推进订单状态，必要时执行账务入账或解冻，并创建商户通知任务。</li>
 *     <li>按 PSP 协议返回成功或失败响应，同时记录完整回调日志。</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PspCallbackService {
    private static final long CALLBACK_ACCOUNT_CACHE_SECONDS = 60L;

    private final List<PspCallbackAdapter> adapters;
    private final PspCallbackRequestFactory requestFactory;
    private final PspCallbackOrderResolver orderResolver;
    private final PspCallbackOrderProcessor orderProcessor;
    private final PspCallbackLogRecorder logRecorder;
    private final PspCallbackNotifyCreator notifyCreator;
    private final LedgerPostingService ledgerPostingService;
    private final PspCallbackValidator callbackValidator;
    private final PayinOrderService payinOrderService;
    private final PspCallbackIpWhitelistService pspCallbackIpWhitelistService;
    private final PspCallbackAckMapper ackMapper;
    private final PspAccountDao pspAccountDao;
    private final RedisUtils redisUtils;

    /**
     * 处理 PSP 代收回调。
     *
     * @param pspAccountNo 回调路径中的 PSP 账号路由号
     * @param request HTTP 原始请求
     * @param rawBody 原始请求体
     * @return PSP 协议响应
     */
    @Transactional(rollbackFor = Exception.class)
    public PspCallbackResponse handlePayCallback(String pspAccountNo, HttpServletRequest request, String rawBody) {
        return handle(pspAccountNo, BizTypeEnum.PAYIN_ORDER.code(), request, rawBody);
    }

    /**
     * 处理 PSP 代付回调。
     *
     * @param pspAccountNo 回调路径中的 PSP 账号路由号
     * @param request HTTP 原始请求
     * @param rawBody 原始请求体
     * @return PSP 协议响应
     */
    @Transactional(rollbackFor = Exception.class)
    public PspCallbackResponse handlePayoutCallback(String pspAccountNo, HttpServletRequest request, String rawBody) {
        return handle(pspAccountNo, BizTypeEnum.PAYOUT_ORDER.code(), request, rawBody);
    }

    /**
     * PSP 回调统一入口。
     *
     * <p>处理顺序是：账号定位 -> IP 白名单 -> 适配器解析 -> 验签 -> 查订单/业务校验 -> 幂等处理。
     */
    private PspCallbackResponse handle(String pspAccountNo, String bizType, HttpServletRequest servletRequest, String rawBody) {
        PspCallbackRequest request = requestFactory.create(pspAccountNo, bizType, servletRequest, rawBody);
        PspCallbackContext context = PspCallbackContext.of(pspAccountNo, bizType, request);
        String failResponse = PspCallbackResponse.DEFAULT_FAIL_BODY;
        try {
            // 根据 pspAccountNo 定位 PSP 账号配置
            Result<CallbackAccount> accountResult = resolveCallbackAccount(pspAccountNo);
            if (accountResult.isFail()) {
                return finishFailed(context, accountResult, failResponse);
            }

            CallbackAccount account = accountResult.getData();
            request.setPspCode(account.pspCode());
            request.setApiSecret(account.apiSecret());
            context.setTenantId(account.tenantId());
            context.setPspId(account.pspId());
            context.setPspAccountId(account.pspAccountId());
            context.setPspCode(account.pspCode());

            // 校验来源 IP 用 pspCode 或 pspAccountId 找白名单. 不通过直接拒绝。
            // 选择根据 pspCode 找对应适配器，比如 WorldPspCallbackAdapter。
            Result<PspCallbackContext> prepared = prepare(context);
            if (prepared.isFail()) {
                return finishFailed(context, prepared, failResponse);
            }

            context = prepared.getData();
            failResponse = failBody(context.getResult());

            // 验签阶段：使用 pspAccountNo 对应账号的密钥验证 PSP 签名。
            Result<PspCallbackContext> signed = verifySignature(context);
            if (signed.isFail()) {
                return finishFailed(context, signed, failResponse);
            }

            // 业务校验阶段：定位订单并校验终态回调字段。
            Result<PspCallbackContext> verified = resolveAndValidate(context, account);
            if (verified.isFail()) {
                return finishFailed(context, verified, failResponse);
            }

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
            boolean rollback = context.isOrderChanged();
            if (rollback) {
                rollbackIfActive();
            }
            if (context.getLog() == null) {
                context.setLog(logRecorder.failed(context.getPspCode(), bizType, request, ex,
                        context.getTenantId(), context.getPspId()));
            }
            logRecorder.finish(
                    context.getLog(),
                    PspCallbackVerifyStatusEnum.FAILED.code(),
                    PspCallbackProcessStatusEnum.FAILED.code(),
                    ex.getMessage()
            );
            PspCallbackResponse response = ackMapper.toResponse(Result.fail(PspCallbackAckMapper.SYSTEM_ERROR), failResponse);
            logFailure(context.getPspCode(), bizType, response.status(), ex);
            if (rollback) {
                throw new PspCallbackException(response.status(), ex.getMessage(), response.body());
            }
            return response;
        }
    }

    /**
     * 准备阶段：校验 IP 白名单、选择适配器、解析原始回调报文。
     *
     * <p>本阶段只做来源和报文格式处理，不查询订单，也不修改任何业务数据。
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

        Result<PspCallbackResult> parsed = BizTypeEnum.PAYIN_ORDER.matches(context.getBizType())
                ? adapter.parsePayCallback(request)
                : adapter.parsePayoutCallback(request);
        if (parsed.isFail()) {
            return Result.fail(parsed.getMsg());
        }

        PspCallbackResult result = parsed.getData();
        if (result == null) {
            return Result.fail(PspCallbackAckMapper.PARSE_FAILED);
        }
        if (StringUtils.isBlank(result.getPspCode())) {
            result.setPspCode(request.getPspCode());
        }
        context.setResult(result);
        return Result.success(context);
    }

    /**
     * 验签阶段：使用 pspAccountNo 对应账号的密钥验证 PSP 签名。
     *
     * <p>验签失败直接拒绝，不查询订单、不推进状态、不触发账务。
     */
    private Result<PspCallbackContext> verifySignature(PspCallbackContext context) {
        PspCallbackAdapter adapter = findAdapter(context.getRequest().getPspCode());
        if (adapter == null) {
            return Result.fail(PspCallbackAckMapper.ADAPTER_NOT_FOUND);
        }

        Result<Void> signed = adapter.verifySign(context.getRequest());
        if (signed.isFail()) {
            return Result.fail(StringUtils.defaultIfBlank(signed.getMsg(), PspCallbackAckMapper.SIGN_INVALID));
        }
        return Result.success(context);
    }

    /**
     * 验签后的业务校验阶段：定位订单并校验终态回调字段。
     */
    private Result<PspCallbackContext> resolveAndValidate(PspCallbackContext context, CallbackAccount account) {
        try {
            PspCallbackOrder order = orderResolver.resolve(
                    context.getBizType(),
                    context.getResult(),
                    account.pspAccountId(),
                    account.pspCode(),
                    account.apiSecret()
            );
            context.setOrder(order);
            context.setLog(logRecorder.received(context.getRequest(), context.getResult(), order));
        } catch (PspCallbackBizException ex) {
            return Result.fail(ex.getAckCode());
        }

        context.setTerminal(PspCallbackUtils.isTerminal(context.getResult().getOrderStatus()));
        if (!context.isTerminal()) {
            return Result.success(context);
        }

        Result<Void> validated = callbackValidator.validateTerminalCallbackResult(
                context.getResult(),
                context.getOrder()
        );
        if (validated.isFail()) {
            return Result.fail(validated.getMsg());
        }
        return Result.success(context);
    }

    /**
     * 处理阶段：幂等推进订单，终态变更后执行账务处理并创建商户通知。
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
                if (BizTypeEnum.PAYIN_ORDER.matches(context.getBizType())
                        && PayinOrderStatusEnum.SUCCESS.code().equals(PspCallbackUtils.normalizeStatus(context.getResult().getOrderStatus()))) {
                    payinOrderService.onPaySuccessPosted(context.getOrder().id());
                }
                notifyCreator.create(context.getBizType(), context.getResult(), context.getOrder(), context.getLog());
            }

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
        } catch (PspCallbackBizException ex) {
            return Result.fail(ex.getAckCode());
        }
    }

    /**
     * 阶段失败统一收口：补写失败日志，并按错误码映射 PSP 响应。
     */
    private PspCallbackResponse finishFailed(PspCallbackContext context, Result<?> failure, String failResponse) {
        if (context.getLog() == null) {
            context.setLog(logRecorder.failed(context.getPspCode(), context.getBizType(), context.getRequest(),
                    new IllegalStateException(failureMessage(failure)),
                    context.getTenantId(), context.getPspId()));
        }
        String code = failureMessage(failure);
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
     * 根据 pspAccountNo 读取回调账号上下文。
     *
     * <p>账号信息来自 Redis 缓存，未命中时通过 psp_account 和 psp_provider 联表查询。
     */
    private Result<CallbackAccount> resolveCallbackAccount(String pspAccountNo) {
        if (StringUtils.isBlank(pspAccountNo)) {
            return Result.fail(PspCallbackAckMapper.ADAPTER_NOT_FOUND);
        }
        String trimmedAccountNo = StringUtils.trim(pspAccountNo);
        String cacheKey = PaymentRedisKeys.getPspCallbackAccountKey(trimmedAccountNo);
        PspCallbackAccount account = getCachedCallbackAccount(cacheKey);
        if (account == null) {
            account = pspAccountDao.selectCallbackAccount(trimmedAccountNo);
            cacheCallbackAccount(cacheKey, account);
        }
        if (account == null || StringUtils.isBlank(account.getPspCode())) {
            return Result.fail(PspCallbackAckMapper.ADAPTER_NOT_FOUND);
        }
        CallbackAccount data = new CallbackAccount(
                account.getTenantId(),
                account.getPspId(),
                account.getPspAccountId(),
                account.getPspCode(),
                account.getApiSecret()
        );
        return Result.success(data);
    }

    private PspCallbackAccount getCachedCallbackAccount(String cacheKey) {
        try {
            return redisUtils.get(cacheKey, PspCallbackAccount.class);
        } catch (Exception ex) {
            log.warn("Get PSP callback account cache failed: {}", ex.getMessage());
            return null;
        }
    }

    private void cacheCallbackAccount(String cacheKey, PspCallbackAccount account) {
        if (account == null || StringUtils.isBlank(account.getPspCode())) {
            return;
        }
        try {
            redisUtils.set(cacheKey, account, CALLBACK_ACCOUNT_CACHE_SECONDS);
        } catch (Exception ex) {
            log.warn("Set PSP callback account cache failed: {}", ex.getMessage());
        }
    }

    /**
     * 根据失败原因决定日志里的验签状态。
     *
     * <p>金额、币种、PSP 编码和状态不支持属于验签后的业务拒绝，不应记为签名失败。
     */
    private String verifyStatusForFailure(String code) {
        return switch (StringUtils.defaultString(code)) {
            case PspCallbackAckMapper.AMOUNT_MISMATCH,
                 PspCallbackAckMapper.CURRENCY_MISMATCH,
                 PspCallbackAckMapper.PSP_CODE_MISMATCH,
                 PspCallbackAckMapper.UNSUPPORTED_STATUS -> PspCallbackVerifyStatusEnum.SUCCESS.code();
            default -> PspCallbackVerifyStatusEnum.FAILED.code();
        };
    }

    private String failBody(PspCallbackResult result) {
        if (result == null) {
            return PspCallbackResponse.DEFAULT_FAIL_BODY;
        }
        return StringUtils.defaultIfBlank(result.getFailResponse(), PspCallbackResponse.DEFAULT_FAIL_BODY);
    }

    private PspCallbackAdapter findAdapter(String pspCode) {
        return adapters.stream()
                .filter(adapter -> adapter.supports(pspCode))
                .findFirst()
                .orElse(null);
    }

    /**
     * 根据终态执行账务处理。
     */
    private LedgerPostingResult postLedger(String bizType, PspCallbackResult result, PspCallbackOrder order) {
        String status = PspCallbackUtils.normalizeStatus(result.getOrderStatus());
        if (BizTypeEnum.PAYIN_ORDER.matches(bizType) && PspCallbackStatus.SUCCESS.code().equals(status)) {
            return ledgerPostingService.postPaySuccess(paySuccessRequest(result, order));
        }
        if (BizTypeEnum.PAYOUT_ORDER.matches(bizType) && PspCallbackStatus.SUCCESS.code().equals(status)) {
            return ledgerPostingService.postPayoutSuccess(payoutRequest(order));
        }
        if (BizTypeEnum.PAYOUT_ORDER.matches(bizType) && PspCallbackStatus.FAILED.code().equals(status)) {
            return ledgerPostingService.releasePayout(payoutRequest(order));
        }
        return null;
    }

    /**
     * 构建代收成功入账请求；回调金额为空时回退到订单金额。
     */
    private PaySuccessPostingRequest paySuccessRequest(PspCallbackResult result, PspCallbackOrder order) {
        PaySuccessPostingRequest request = new PaySuccessPostingRequest();
        request.setTenantId(order.tenantId());
        request.setMerchantId(order.merchantId());
        request.setMerchantNo(order.merchantNo());
        request.setMerchantAppId(order.merchantAppId());
        request.setMerchantOrderNo(order.merchantOrderNo());
        request.setPspAccountId(order.pspAccountId());
        request.setBizId(order.id());
        request.setPayinOrderNo(order.orderNo());
        request.setCurrency(order.currency());
        request.setAmount(PspCallbackUtils.defaultAmount(result.getAmount(), order.amount()));
        request.setMerchantFeeAmount(order.merchantFeeAmount());
        request.setSettleAmount(order.settleAmount());
        return request;
    }

    /**
     * 构建代付账务请求；成功扣减冻结，失败释放冻结。
     */
    private PayoutPostingRequest payoutRequest(PspCallbackOrder order) {
        PayoutPostingRequest request = new PayoutPostingRequest();
        request.setTenantId(order.tenantId());
        request.setMerchantId(order.merchantId());
        request.setMerchantNo(order.merchantNo());
        request.setMerchantAppId(order.merchantAppId());
        request.setMerchantOrderNo(order.merchantOrderNo());
        request.setPspAccountId(order.pspAccountId());
        request.setBizId(order.id());
        request.setPayoutOrderNo(order.orderNo());
        request.setCurrency(order.currency());
        request.setAmount(order.amount());
        request.setMerchantFeeAmount(order.merchantFeeAmount());
        request.setTotalDebitAmount(order.totalDebitAmount());
        return request;
    }

    /**
     * 当前事务中订单已变更但后续步骤失败时，标记事务回滚，避免订单状态和账务不一致。
     */
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

    private record CallbackAccount(Long tenantId, Long pspId, Long pspAccountId, String pspCode, String apiSecret) {
    }
}
