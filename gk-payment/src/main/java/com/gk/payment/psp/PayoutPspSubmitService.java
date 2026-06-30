package com.gk.payment.psp;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gk.common.enums.PayDirectionEnum;
import com.gk.infra.config.service.GkSysParamsConfigService;
import com.gk.infra.utils.AsynUtils;
import com.gk.ledger.exception.InsufficientLedgerBalanceException;
import com.gk.ledger.posting.LedgerPostingResult;
import com.gk.ledger.posting.PayoutPostingRequest;
import com.gk.ledger.service.LedgerPostingService;
import com.gk.openapi.error.ApiErrorCode;
import com.gk.openapi.error.ApiException;
import com.gk.payment.dao.PayoutOrderDao;
import com.gk.payment.entity.PayoutOrderEntity;
import com.gk.payment.enums.PayoutOrderStatusEnum;
import com.gk.payment.service.OrderStatusLogService;
import com.gk.psp.dispatch.PspPayoutDispatchResult;
import com.gk.psp.dispatch.PspPayoutDispatchService;
import com.gk.psp.route.PspRouteResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;

/**
 * 代付提交 PSP 编排服务。
 * <p>
 * 本服务是同步 OpenAPI 提交和异步 outbox 提交的统一入口：先冻结商户资金，再在数据库事务外调用 PSP，
 * 最后根据 adapter 归一化后的提交结果更新订单。只有 PSP 明确拒绝时才释放冻结资金；提交结果未知时
 * 保留冻结资金，并等待主动查单或回调确认。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PayoutPspSubmitService {
    private static final String PSP_SUBMIT_UNKNOWN_REASON = "PSP payout submit result unknown, waiting for query";

    private final PayoutOrderDao payoutOrderDao;
    private final LedgerPostingService ledgerPostingService;
    private final PspPayoutDispatchService pspPayoutDispatchService;
    private final OrderStatusLogService orderStatusLogService;
    private final GkSysParamsConfigService configService;
    private final TransactionTemplate transactionTemplate;

    /**
     * 冻结商户资金并提交代付订单到 PSP。
     * <p>
     * OpenAPI 和 outbox 都应优先调用该方法，保证冻结、提交、释放冻结和 UNKNOWN 处理逻辑集中在一处。
     */
    public PayoutOrderEntity freezeAndSubmit(PayoutOrderEntity order, PspRouteResult route, SubmitContext context) {
        PayoutOrderEntity frozen = freezeIfNeeded(order, context);
        if (shouldSkipPspSubmit(frozen)) {
            return frozen;
        }
        return submit(frozen, route, context);
    }

    /**
     * 提交已经冻结资金的代付订单到 PSP，并处理归一化后的提交结果。
     * <p>
     * PSP 网络调用刻意放在数据库事务外执行；adapter 返回结果后，再开启短事务锁定订单并落库。
     */
    public PayoutOrderEntity submit(PayoutOrderEntity order, PspRouteResult route, SubmitContext context) {
        if (order == null || order.getId() == null) {
            throw new ApiException(ApiErrorCode.INVALID_REQUEST, "Payout order is required");
        }
        if (route == null) {
            // 路由缺失不能证明商户订单失败，保留为可查单/可重试状态。
            markSubmitUnknown(order.getId(), new ApiException(ApiErrorCode.SERVICE_NOT_READY, "PSP route is unavailable"), context);
            return payoutOrderDao.selectById(order.getId());
        }
        try {
            // adapter 负责 PSP 协议细节，并输出 ACCEPTED/REJECTED/UNKNOWN 三态。
            PspPayoutDispatchResult dispatchResult = pspPayoutDispatchService.dispatch(PspOrderRequests.fromPayoutOrder(order), route);
            transactionTemplate.executeWithoutResult(status -> applyDispatchResult(order.getId(), dispatchResult, context));
        } catch (ApiException ex) {
            markSubmitUnknown(order.getId(), ex, context);
        } catch (Exception ex) {
            log.warn("Payout PSP submit result unknown, payoutOrderNo={}, err={}", order.getPayoutOrderNo(), ex.getMessage(), ex);
            markSubmitUnknown(order.getId(), ex, context);
        }
        return payoutOrderDao.selectById(order.getId());
    }

    /**
     * 根据 PSP 提交三态结果更新代付订单。
     * <p>
     * ACCEPTED 和 UNKNOWN 都保持非终态；只有 REJECTED 才允许订单失败并释放冻结资金。
     */
    private void applyDispatchResult(Long orderId, PspPayoutDispatchResult result, SubmitContext context) {
        PayoutOrderEntity order = lockOrder(orderId);
        String fromStatus = order.getStatus();
        order.setPspRequestNo(result.getPspRequestNo());
        order.setPspOrderNo(result.getPspOrderNo());
        order.setPspRawStatus(result.getRawStatus());
        if (result.isAccepted()) {
            // ACCEPTED 只代表 PSP 已受理提交，不代表代付已经终态成功。
            order.setStatus(PayoutOrderStatusEnum.PROCESSING.code());
            order.setPspStatus(PayoutOrderStatusEnum.PROCESSING.code());
            order.setSubmittedAt(Instant.now());
            order.setNextQueryAt(Instant.now().plusSeconds(firstQueryDelaySeconds()));
            payoutOrderDao.updateById(order);
            recordStatusChange(order, fromStatus, order.getStatus(), "PSP_SUBMIT", null, context);
            return;
        }
        if (result.isUnknown()) {
            // UNKNOWN 可能已经到达 PSP；查单或回调证明失败前，绝不能释放冻结资金。
            applySubmitUnknown(order, result, fromStatus, context);
            return;
        }

        // REJECTED 是 PSP 明确拒绝，此时订单可以失败，并允许释放冻结资金。
        String reason = StringUtils.defaultIfBlank(
                result.getErrorMessage(),
                StringUtils.defaultIfBlank(result.getResponseMessage(), "PSP payout submit failed")
        );
        order.setStatus(PayoutOrderStatusEnum.FAILED.code());
        order.setPspStatus(PayoutOrderStatusEnum.FAILED.code());
        order.setFailCode(result.getErrorCode());
        order.setFailMsg(StringUtils.left(reason, 512));
        order.setStatusReason(reason);
        order.setFailedAt(Instant.now());
        releasePayout(order);
        payoutOrderDao.updateById(order);
        recordStatusChange(order, fromStatus, order.getStatus(), "PSP_SUBMIT_FAILED", order.getStatusReason(), context);
    }

    /**
     * 将提交异常按 UNKNOWN 处理。
     * <p>
     * 超时、解析失败、响应不可信或 adapter 异常，都不能证明 PSP 没有受理该订单。
     */
    private void markSubmitUnknown(Long orderId, Exception ex, SubmitContext context) {
        transactionTemplate.executeWithoutResult(status -> {
            PayoutOrderEntity order = lockOrder(orderId);
            if (isTerminalStatus(order)) {
                return;
            }
            String fromStatus = order.getStatus();
            applySubmitUnknown(order,
                    ex instanceof ApiException apiException ? apiException.getErrorCode().name() : ex.getClass().getSimpleName(),
                    ex.getMessage(),
                    fromStatus,
                    context);
        });
    }

    /**
     * 在订单尚未冻结时冻结商户资金。
     * <p>
     * 通过订单行锁保证重试幂等，不会重复冻结。只有余额不足属于明确业务失败；系统异常直接抛出，
     * 交给调用方或 outbox 重试，不能误把订单标记为失败。
     */
    private PayoutOrderEntity freezeIfNeeded(PayoutOrderEntity order, SubmitContext context) {
        if (order == null || order.getId() == null) {
            throw new ApiException(ApiErrorCode.INVALID_REQUEST, "Payout order is required");
        }
        SubmitContext safeContext = context == null ? SubmitContext.system(order.getAppId(), null) : context;
        return transactionTemplate.execute(status -> {
            PayoutOrderEntity current = lockOrder(order.getId());
            if (shouldSkipPspSubmit(current) || StringUtils.isNotBlank(current.getHoldNo())) {
                return current;
            }
            try {
                // 冻结资金和保存 holdNo 在同一事务内完成，避免出现账务已冻结但订单未记录的情况。
                LedgerPostingResult result = ledgerPostingService.freezePayout(payoutPostingRequest(current));
                String fromStatus = current.getStatus();
                current.setHoldNo(result.getHoldNo());
                current.setFreezeJournalNo(result.getJournalNo());
                current.setStatus(PayoutOrderStatusEnum.FROZEN.code());
                current.setStatusReason(null);
                payoutOrderDao.updateById(current);
                recordStatusChange(current, fromStatus, current.getStatus(), "PAYOUT_FROZEN", null, safeContext);
                return current;
            } catch (InsufficientLedgerBalanceException ex) {
                markFreezeFailed(current, ApiErrorCode.INSUFFICIENT_BALANCE.getMessage(),
                        ApiErrorCode.INSUFFICIENT_BALANCE.name(), safeContext);
                if (safeContext.throwOnFreezeFailure()) {
                    throw new ApiException(ApiErrorCode.INSUFFICIENT_BALANCE, ex);
                }
                return current;
            } catch (ApiException ex) {
                throw ex;
            } catch (Exception ex) {
                throw new ApiException(ApiErrorCode.SYSTEM_ERROR, ex);
            }
        });
    }

    /**
     * 标记冻结阶段的明确业务失败。
     * <p>
     * 当前主要用于余额不足；可重试的账务或系统异常不能走该路径。
     */
    private void markFreezeFailed(PayoutOrderEntity order, String reason, String failCode, SubmitContext context) {
        String fromStatus = order.getStatus();
        String message = StringUtils.defaultIfBlank(reason, "Payout freeze failed");
        order.setStatus(PayoutOrderStatusEnum.FAILED.code());
        order.setPspStatus(PayoutOrderStatusEnum.FAILED.code());
        order.setStatusReason(message);
        order.setFailCode(failCode);
        order.setFailMsg(StringUtils.left(message, 512));
        order.setFailedAt(Instant.now());
        payoutOrderDao.updateById(order);
        recordStatusChange(order, fromStatus, order.getStatus(), "PAYOUT_FREEZE_FAILED", message, context);
    }

    /**
     * 将 adapter 返回的 UNKNOWN 提交结果写入订单。
     */
    private void applySubmitUnknown(PayoutOrderEntity order,
                                    PspPayoutDispatchResult result,
                                    String fromStatus,
                                    SubmitContext context) {
        applySubmitUnknown(order,
                StringUtils.defaultIfBlank(result.getErrorCode(), result.getResponseCode()),
                StringUtils.defaultIfBlank(result.getErrorMessage(), result.getResponseMessage()),
                fromStatus,
                context);
    }

    /**
     * 将异常型 UNKNOWN 提交结果写入订单，并安排首次主动查单时间。
     */
    private void applySubmitUnknown(PayoutOrderEntity order,
                                    String failCode,
                                    String failMessage,
                                    String fromStatus,
                                    SubmitContext context) {
        order.setStatus(PayoutOrderStatusEnum.PROCESSING.code());
        order.setPspStatus(PayoutOrderStatusEnum.PROCESSING.code());
        order.setStatusReason(PSP_SUBMIT_UNKNOWN_REASON);
        order.setFailCode(failCode);
        order.setFailMsg(StringUtils.left(failMessage, 512));
        if (order.getSubmittedAt() == null) {
            order.setSubmittedAt(Instant.now());
        }
        order.setNextQueryAt(Instant.now().plusSeconds(firstQueryDelaySeconds()));
        payoutOrderDao.updateById(order);
        recordStatusChange(order, fromStatus, order.getStatus(), "PSP_SUBMIT_UNKNOWN", order.getStatusReason(), context);
    }

    /**
     * 按订单主键加行锁，保护冻结和 PSP 提交结果回写的并发安全。
     */
    private PayoutOrderEntity lockOrder(Long orderId) {
        PayoutOrderEntity order = payoutOrderDao.selectOne(new QueryWrapper<PayoutOrderEntity>()
                .eq("id", orderId)
                .last("limit 1 for update"));
        if (order == null) {
            throw new ApiException(ApiErrorCode.INVALID_REQUEST, "Payout order not found");
        }
        return order;
    }

    private boolean isTerminalStatus(PayoutOrderEntity order) {
        return PayoutOrderStatusEnum.SUCCESS.code().equals(order.getStatus())
                || PayoutOrderStatusEnum.FAILED.code().equals(order.getStatus())
                || PayoutOrderStatusEnum.CANCELLED.code().equals(order.getStatus());
    }

    /**
     * 判断当前订单是否应跳过 PSP 自动提交。
     * <p>
     * FROZEN 不在这里拦截，因为冻结完成后仍需要继续提交 PSP；PROCESSING 表示已提交或提交结果未知；
     * MANUAL_REVIEW 和终态订单都不应被自动提交。
     */
    private boolean shouldSkipPspSubmit(PayoutOrderEntity order) {
        return PayoutOrderStatusEnum.PROCESSING.code().equals(order.getStatus())
                || PayoutOrderStatusEnum.MANUAL_REVIEW.code().equals(order.getStatus())
                || isTerminalStatus(order);
    }

    /**
     * PSP 明确拒绝后释放冻结资金。
     * <p>
     * 解冻失败只记录告警，不能把 PSP 明确拒绝反向改成 UNKNOWN；后续可由运营或补偿任务根据
     * holdNo/releaseJournalNo 继续处理。
     */
    private void releasePayout(PayoutOrderEntity order) {
        if (StringUtils.isBlank(order.getHoldNo()) || StringUtils.isNotBlank(order.getReleaseJournalNo())) {
            return;
        }
        try {
            LedgerPostingResult result = ledgerPostingService.releasePayout(payoutPostingRequest(order));
            order.setReleaseJournalNo(result.getJournalNo());
        } catch (Exception ex) {
            log.warn("Release payout after PSP submit failure failed, payoutOrderNo={}, err={}",
                    order.getPayoutOrderNo(), ex.getMessage(), ex);
        }
    }

    /**
     * 根据订单落库快照构建账务冻结/解冻请求。
     */
    private PayoutPostingRequest payoutPostingRequest(PayoutOrderEntity order) {
        PayoutPostingRequest request = new PayoutPostingRequest();
        request.setTenantId(order.getTenantId());
        request.setMerchantId(order.getMerchantId());
        request.setMerchantNo(order.getMerchantNo());
        request.setMerchantAppId(order.getMerchantAppId());
        request.setMerchantOrderNo(order.getMerchantOrderNo());
        request.setPspAccountId(order.getPspAccountId());
        request.setBizId(order.getId());
        request.setPayoutOrderNo(order.getPayoutOrderNo());
        request.setCurrency(order.getCurrency());
        request.setAmount(order.getAmount());
        request.setMerchantFeeAmount(order.getMerchantFeeAmount());
        request.setTotalDebitAmount(order.getTotalDebitAmount());
        return request;
    }

    /**
     * 获取 PSP 提交后的首次查单延迟，配置异常时使用 60 秒兜底。
     */
    private long firstQueryDelaySeconds() {
        long seconds = configService.payoutSubmitConfig().getFirstQueryDelaySeconds();
        return seconds <= 0 ? 60L : seconds;
    }

    /**
     * 异步记录订单状态变更，避免状态日志写入阻塞主支付链路。
     */
    private void recordStatusChange(PayoutOrderEntity order,
                                    String fromStatus,
                                    String toStatus,
                                    String eventType,
                                    String reason,
                                    SubmitContext context) {
        SubmitContext safeContext = context == null ? SubmitContext.system(order.getAppId(), null) : context;
        AsynUtils.execute("Order status log", () -> orderStatusLogService.recordChange(
                PayDirectionEnum.PAYOUT.code(),
                order.getTenantId(),
                order.getMerchantId(),
                order.getId(),
                order.getPayoutOrderNo(),
                fromStatus,
                toStatus,
                eventType,
                reason,
                safeContext.operatorType(),
                StringUtils.defaultIfBlank(safeContext.appId(), order.getAppId()),
                order.getMerchantOrderNo(),
                safeContext.traceId()
        ));
    }

    /**
     * 同步 OpenAPI 和异步 outbox 共享的提交上下文。
     * <p>
     * throwOnFreezeFailure 用于区分调用场景：OpenAPI 需要把冻结失败明确返回给商户；outbox 对明确业务失败
     * 可以标记订单后结束任务。
     */
    public record SubmitContext(String operatorType, String appId, String traceId, boolean throwOnFreezeFailure) {
        public static SubmitContext system(String appId, String traceId) {
            return new SubmitContext("SYSTEM", appId, traceId, false);
        }

        public SubmitContext withFreezeFailureException() {
            return new SubmitContext(operatorType, appId, traceId, true);
        }
    }
}
