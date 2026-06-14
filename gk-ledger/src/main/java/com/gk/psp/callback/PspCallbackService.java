package com.gk.psp.callback;

import com.gk.common.enums.BizTypeEnum;
import com.gk.infra.ipwhitelist.service.PspCallbackIpWhitelistService;
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
 * PSP 回调处理编排服务。
 * <p>
 * 这个类不负责具体某个 PSP 的报文解析，也不直接写订单 SQL 细节；它负责把回调主链路串起来：
 * 选择 PSP 适配器、解析报文、定位平台订单、验签、校验终态金额/币种、更新订单状态、
 * 执行账务入账或解冻、创建商户通知任务，并记录 PSP 回调日志。
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
    private final PspCallbackIpWhitelistService pspCallbackIpWhitelistService;

    /**
     * 处理 PSP 代收回调。
     * <p>
     * controller 收到代收回调后调用本方法，bizType 固定为 PAY_ORDER。
     *
     * @param pspCode PSP 编码，用于选择对应回调适配器
     * @param request HTTP 请求对象，包含 headers、query、form 等原始回调信息
     * @param rawBody 原始请求体，用于验签、日志和部分 JSON 回调解析
     * @return 返回给 PSP 的响应内容，通常是 success/fail 或 PSP 要求的固定字符串
     */
    @Transactional(rollbackFor = Exception.class)
    public String handlePayCallback(String pspCode, HttpServletRequest request, String rawBody) {
        return handle(pspCode, BizTypeEnum.PAY_ORDER.code(), request, rawBody);
    }

    /**
     * 处理 PSP 代付回调。
     * <p>
     * controller 收到代付回调后调用本方法，bizType 固定为 PAYOUT_ORDER。
     *
     * @param pspCode PSP 编码，用于选择对应回调适配器
     * @param request HTTP 请求对象，包含 headers、query、form 等原始回调信息
     * @param rawBody 原始请求体，用于验签、日志和部分 JSON 回调解析
     * @return 返回给 PSP 的响应内容，通常是 success/fail 或 PSP 要求的固定字符串
     */
    @Transactional(rollbackFor = Exception.class)
    public String handlePayoutCallback(String pspCode, HttpServletRequest request, String rawBody) {
        return handle(pspCode, BizTypeEnum.PAYOUT_ORDER.code(), request, rawBody);
    }

    /**
     * PSP 回调统一处理主流程。
     * <p>
     * 代收和代付回调都会走这里，通过 bizType 区分订单类型。方法要保证：
     * 回调日志可追溯、重复回调幂等、终态回调才触发账务、订单状态和账务凭证一致。
     */
    private String handle(String pspCode, String bizType, HttpServletRequest servletRequest, String rawBody) {
        // 1. 根据 PSP 编码找到对应适配器；适配器负责该 PSP 的报文解析和验签规则。
        PspCallbackAdapter adapter = findAdapter(pspCode);
        if (adapter == null) {
            return "fail";
        }

        // 2. 把 HTTP 请求转换成内部统一回调请求模型，保留原始 body、headers、参数等信息。
        PspCallbackRequest request = requestFactory.create(pspCode, bizType, servletRequest, rawBody);
        // 回调日志实体稍后创建；如果解析或定位订单失败，catch 中会记录失败日志。
        PspCallbackLogEntity logEntity = null;
        // 标记订单是否已经发生状态变更；若后续账务或通知失败，需要把事务标记回滚。
        boolean orderChanged = false;
        try {
            if (!pspCallbackIpWhitelistService.isPspCallbackAllowed(request.getPspCode(), request.getClientIp())) {
                throw new IllegalStateException("PSP callback IP is not allowed");
            }
            // 3. 使用 PSP 适配器把原始回调解析成统一结果模型，包括订单号、状态、金额、币种等。
            PspCallbackResult result = parse(adapter, bizType, request);
            // 4. 根据回调结果定位平台侧订单，并拿到订单快照、商户、金额、手续费、PSP 账户等信息。
            PspCallbackOrder order = orderResolver.resolve(bizType, result);
            // 5. 回填 PSP 账户密钥到请求模型，后续验签需要使用它。
            request.setApiSecret(order.apiSecret());
            // 6. 先记录“已收到回调”的日志，后面再补验签状态和处理状态。
            logEntity = logRecorder.received(request, result, order);

            // 7. 验签失败不更新订单、不入账，只把回调日志标记为验签失败，并按 PSP 协议返回失败响应。
            if (!adapter.verifySign(request)) {
                logRecorder.finish(logEntity, PspCallbackVerifyStatusEnum.FAILED.code(), PspCallbackProcessStatusEnum.FAILED.code(), "Invalid PSP callback signature");
                return result.getFailResponse();
            }

            // 8. 判断 PSP 回调状态是否为终态；只有 SUCCESS/FAILED/CLOSED 等终态才需要金额/币种强校验和账务动作。
            boolean terminal = PspCallbackUtils.isTerminal(result.getOrderStatus());
            if (terminal) {
                // 9. 终态回调必须校验金额、币种、PSP 编码等关键字段，避免错单或金额不一致入账。
                callbackValidator.validateTerminalCallback(bizType, result, order);
            }
            // 10. 按回调状态更新平台订单；重复回调、已终态订单会被识别为未变更。
            orderChanged = orderProcessor.process(bizType, result, order, null);
            if (orderChanged && terminal) {
                // 11. 只有订单真的转为终态时才执行账务，避免处理中状态或重复回调重复入账。
                LedgerPostingResult postingResult = postLedger(bizType, result, order);
                // 12. 将账务凭证号回填到订单，保证订单和账务流水可以互相追溯。
                orderProcessor.attachPostingResult(bizType, order.id(), result.getOrderStatus(), postingResult);
                if (BizTypeEnum.PAY_ORDER.matches(bizType)
                        && PayOrderStatusEnum.SUCCESS.code().equals(PspCallbackUtils.normalizeStatus(result.getOrderStatus()))) {
                    // 13. 代收成功入账后计算待结算释放时间；AUTO 结算且到期时会继续释放到可用余额。
                    payOrderService.onPaySuccessPosted(order.id());
                }
                // 14. 创建商户异步通知任务，由 merchantNotifyTask 定时发送或重试。
                notifyCreator.create(bizType, result, order, logEntity);
            }

            // 15. 标记回调日志处理完成；重复回调或无状态变化记为 IGNORED。
            logRecorder.finish(logEntity, PspCallbackVerifyStatusEnum.SUCCESS.code(), orderChanged ? PspCallbackProcessStatusEnum.SUCCESS.code() : PspCallbackProcessStatusEnum.IGNORED.code(), null);
            // 16. 返回 PSP 适配器解析出的成功响应，满足不同 PSP 对回调响应内容的要求。
            return result.getSuccessResponse();
        } catch (Exception ex) {
            if (orderChanged) {
                // 订单已经更新但后续账务/通知失败时，回滚整个事务，避免订单终态和账务不一致。
                rollbackIfActive();
            }
            if (logEntity == null) {
                // 如果还没来得及创建正常回调日志，就创建一条失败日志，避免异常回调丢失排查线索。
                logEntity = logRecorder.failed(pspCode, bizType, request, ex);
            }
            // 统一把回调日志标记为失败，并把异常信息写入日志。
            logRecorder.finish(logEntity, PspCallbackVerifyStatusEnum.FAILED.code(), PspCallbackProcessStatusEnum.FAILED.code(), ex.getMessage());
            return "fail";
        }
    }

    /**
     * 将当前 Spring 事务标记为回滚。
     * <p>
     * 用于订单已经发生状态变更，但后续账务或通知创建失败的场景，避免部分成功。
     */
    private void rollbackIfActive() {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        }
    }

    /**
     * 按业务类型调用 PSP 适配器解析回调报文。
     *
     * @param adapter PSP 回调适配器
     * @param bizType 业务类型：PAY_ORDER 或 PAYOUT_ORDER
     * @param request 统一回调请求模型
     * @return 统一回调结果模型
     */
    private PspCallbackResult parse(PspCallbackAdapter adapter, String bizType, PspCallbackRequest request) {
        if (BizTypeEnum.PAY_ORDER.matches(bizType)) {
            return adapter.parsePayCallback(request);
        }
        return adapter.parsePayoutCallback(request);
    }

    /**
     * 根据 PSP 编码选择支持该 PSP 的回调适配器。
     * <p>
     * 每个 PSP 的签名、字段命名、状态码都可能不同，因此通过适配器隔离差异。
     *
     * @param pspCode PSP 编码
     * @return 支持该 PSP 的适配器；没有找到时返回 null
     */
    private PspCallbackAdapter findAdapter(String pspCode) {
        return adapters.stream()
                .filter(adapter -> adapter.supports(pspCode))
                .findFirst()
                .orElse(null);
    }

    /**
     * 根据回调终态执行账务处理。
     * <p>
     * 代收成功：系统清算到商户待结算/手续费收入。
     * 代付成功：消费冻结金额，确认商户出款和手续费。
     * 代付失败：释放冻结金额回商户可用余额。
     *
     * @param bizType 业务类型
     * @param result PSP 回调结果
     * @param order 平台订单快照
     * @return 账务处理结果；不需要账务动作时返回 null
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

    /**
     * 构建代收成功入账请求。
     * <p>
     * 金额优先使用 PSP 回调金额；回调未给金额时回退到订单金额。
     *
     * @param result PSP 回调结果
     * @param order 平台订单快照
     * @return 代收成功账务请求
     */
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

    /**
     * 构建代付账务请求。
     * <p>
     * 代付成功和代付失败解冻都需要使用同一批订单金额、手续费和冻结总额信息。
     *
     * @param order 平台订单快照
     * @return 代付账务请求
     */
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
