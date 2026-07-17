package com.gk.payment.psp;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gk.payment.config.PaymentConfigService;
import com.gk.ledger.exception.InsufficientLedgerBalanceException;
import com.gk.ledger.posting.LedgerPostingResult;
import com.gk.ledger.posting.PayoutPostingRequest;
import com.gk.ledger.service.LedgerPostingService;
import com.gk.payment.domain.error.PaymentErrorCode;
import com.gk.payment.domain.error.PaymentException;
import com.gk.payment.dao.PayoutOrderDao;
import com.gk.payment.entity.PayoutOrderEntity;
import com.gk.payment.enums.MerchantOrderStatusEnum;
import com.gk.payment.enums.PayoutOrderStatusEnum;
import com.gk.payment.plan.model.PayoutPlan;
import com.gk.payment.plan.PayoutPlanService;
import com.gk.payment.service.PayoutRouteAttemptService;
import com.gk.payment.state.OrderStateChangeContext;
import com.gk.payment.state.PayoutOrderStateService;
import com.gk.psp.dispatch.PspPayoutDispatchResult;
import com.gk.psp.dispatch.PspPayoutDispatchService;
import com.gk.psp.enums.PspPayoutSubmitStatus;
import com.gk.psp.route.PspRouteResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * 代付提交 PSP 服务。
 * <p>
 * 核心职责：
 * <ul>
 *     <li>提交前冻结商户资金，避免 PSP 已受理但商户余额未锁定。</li>
 *     <li>将每一次 PSP 提交结果写入 payout_route_attempt，保留完整路由尝试链路。</li>
 *     <li>当 PSP 返回余额不足、账号停用等可换路由结果时，切换到下一个可用通道继续提交。</li>
 *     <li>只有最终提交失败且不再切换路由时，才释放已冻结资金。</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PayoutPspSubmitService {
    private static final String PSP_SUBMIT_UNKNOWN_REASON = "PSP payout submit result unknown, waiting for query";
    private static final String PSP_SUBMITTING_REASON = "PSP payout submission in progress";
    private static final String ROUTE_SWITCH_REASON = "PSP route unavailable, switch payout route";
    private static final String ROUTE_UNAVAILABLE_FAILED_REASON = "No available payout route after PSP route unavailable";
    private static final int DEFAULT_MAX_ROUTE_ATTEMPTS = 3;
    private static final int MAX_ROUTE_ATTEMPTS_LIMIT = 10;

    private final PayoutOrderDao payoutOrderDao;
    private final LedgerPostingService ledgerPostingService;
    private final PspPayoutDispatchService pspPayoutDispatchService;
    private final PayoutPlanService payoutPlanService;
    private final PayoutRouteAttemptService payoutRouteAttemptService;
    private final PaymentConfigService configService;
    private final TransactionTemplate transactionTemplate;
    private final PayoutOrderStateService payoutOrderStateService;

    /**
     * 冻结商户资金后提交 PSP。
     * <p>
     * 这是代付出款的推荐入口：冻结失败不会继续请求 PSP，冻结成功后才进入提交和路由切换流程。
     */
    public PayoutOrderEntity freezeAndSubmit(PayoutOrderEntity order, PspRouteResult route, SubmitContext context) {
        PayoutOrderEntity frozen = freezeIfNeeded(order, context);
        if (shouldSkipPspSubmit(frozen)) {
            return frozen;
        }
        return submit(frozen, route, context);
    }

    /**
     * 按当前路由提交 PSP，并在 PSP 明确返回“路由不可用”时尝试切换下一条路由。
     * <p>
     * 这里不负责首次冻结资金，调用方需要确保订单已经冻结或使用 {@link #freezeAndSubmit(PayoutOrderEntity, PspRouteResult, SubmitContext)}。
     */
    public PayoutOrderEntity submit(PayoutOrderEntity order, PspRouteResult route, SubmitContext context) {
        if (order == null || order.getId() == null) {
            throw new PaymentException(PaymentErrorCode.INVALID_REQUEST, "Payout order is required");
        }
        Long orderId = order.getId();
        if (route == null) {
            markRouteUnavailableFailed(orderId, null, context);
            return payoutOrderDao.selectById(orderId);
        }
        PayoutOrderEntity claimedOrder = claimForSubmit(orderId, context);
        if (claimedOrder == null) {
            return payoutOrderDao.selectById(orderId);
        }

        Set<Long> disabledPspIds = new HashSet<>();
        Set<Long> disabledAccountIds = new HashSet<>();
        Set<Long> disabledRouteOptionIds = new HashSet<>();
        PspRouteResult currentRoute = route;
        int maxAttempts = maxRouteAttempts();
        try {
            for (int attemptIndex = 1; attemptIndex <= maxAttempts; attemptIndex++) {
                PayoutOrderEntity currentOrder = payoutOrderDao.selectById(orderId);
                if (currentOrder == null || shouldStopSubmitFlow(currentOrder)) {
                    return currentOrder;
                }

                // PSP 提交在事务外执行，避免外部 HTTP 调用长时间占用数据库行锁。
                PspRouteResult submitRoute = currentRoute;
                PspPayoutDispatchResult dispatchResult =
                        pspPayoutDispatchService.dispatch(PspOrderRequests.fromPayoutOrder(currentOrder), submitRoute);
                SubmitDecision decision = transactionTemplate.execute(
                        status -> applyDispatchResult(orderId, submitRoute, dispatchResult, context)
                );
                if (decision == null || !decision.routeUnavailable()) {
                    return payoutOrderDao.selectById(orderId);
                }

                // 当前 route/account 已经确认不可用，本轮后续解析路由时需要排除，避免反复选中同一条通道。
                addIfNotNull(disabledAccountIds, decision.pspAccountId());
                addIfNotNull(disabledRouteOptionIds, decision.routeOptionId());
                if (attemptIndex >= maxAttempts) {
                    markRouteUnavailableFailed(orderId, dispatchResult, context);
                    return payoutOrderDao.selectById(orderId);
                }

                PayoutPlan nextPlan = resolveNextPlan(currentOrder, disabledPspIds, disabledAccountIds, disabledRouteOptionIds);
                if (nextPlan == null) {
                    markRouteUnavailableFailed(orderId, dispatchResult, context);
                    return payoutOrderDao.selectById(orderId);
                }
                // 切换路由也放在事务内完成，保证订单上的 PSP 路由快照和下一次提交使用的 route 一致。
                currentRoute = transactionTemplate.execute(status -> switchRoute(orderId, nextPlan, context));
                if (currentRoute == null) {
                    return payoutOrderDao.selectById(orderId);
                }
            }
        } catch (PaymentException ex) {
            markSubmitUnknown(orderId, currentRoute, ex, context);
        } catch (Exception ex) {
            log.warn("Payout PSP submit result unknown, payoutOrderNo={}, err={}",
                    claimedOrder.getPayoutOrderNo(), ex.getMessage(), ex);
            markSubmitUnknown(orderId, currentRoute, ex, context);
        }
        return payoutOrderDao.selectById(orderId);
    }

    /**
     * 将 PSP 提交结果落库到订单。
     * <p>
     * 注意：成功、失败、未知、可换路由都会先写 payout_route_attempt，这张表用于还原完整提交链路。
     */
    private SubmitDecision applyDispatchResult(Long orderId,
                                               PspRouteResult route,
                                               PspPayoutDispatchResult result,
                                               SubmitContext context) {
        PayoutOrderEntity order = lockOrder(orderId);
        if (isTerminalStatus(order)) {
            return SubmitDecision.done();
        }

        String fromStatus = order.getStatus();
        // 每一次实际提交 PSP 的结果都记录 attempt，成功也记录，不只是失败才记录。
        payoutRouteAttemptService.recordAttempt(order, route, result);
        order.setPspRequestNo(result.getPspRequestNo());
        order.setPspOrderNo(result.getPspOrderNo());
        order.setPspRawStatus(result.getRawStatus());

        if (result.isAccepted()) {
            order.setStatus(PayoutOrderStatusEnum.PROCESSING.code());
            order.setPspStatus(PayoutOrderStatusEnum.PROCESSING.code());
            order.setMerchantStatusCode(MerchantOrderStatusEnum.PROCESSING.code());
            order.setMerchantStatusReason(MerchantOrderStatusEnum.PROCESSING.statusReason());
            order.setSubmittedAt(Instant.now());
            order.setNextQueryAt(Instant.now().plusSeconds(firstQueryDelaySeconds()));
            payoutOrderDao.updateById(order);
            recordStatusChange(order, fromStatus, order.getStatus(), "PSP_SUBMIT", null, context);
            return SubmitDecision.done();
        }

        if (result.isUnknown()) {
            applySubmitUnknown(order, result, context);
            return SubmitDecision.done();
        }

        if (result.isRouteUnavailable()) {
            // PSP 余额不足、账号停用等可换路由场景：这里只标记原因并返回切换决策，不释放冻结资金。
            String reason = StringUtils.defaultIfBlank(
                    result.getErrorMessage(),
                    StringUtils.defaultIfBlank(result.getResponseMessage(), ROUTE_SWITCH_REASON)
            );
            order.setStatusReason(StringUtils.left(reason, 512));
            order.setMerchantStatusCode(MerchantOrderStatusEnum.PROCESSING.code());
            order.setMerchantStatusReason(MerchantOrderStatusEnum.PROCESSING.statusReason());
            payoutOrderDao.updateById(order);
            return SubmitDecision.routeUnavailable(order.getPspAccountId(), order.getPaymentPlanRouteOptionId());
        }

        String reason = StringUtils.defaultIfBlank(
                result.getErrorMessage(),
                StringUtils.defaultIfBlank(result.getResponseMessage(), "PSP payout submit failed")
        );
        order.setStatus(PayoutOrderStatusEnum.FAILED.code());
        order.setPspStatus(PayoutOrderStatusEnum.FAILED.code());
        order.setFailCode(result.getErrorCode());
        order.setFailMsg(StringUtils.left(reason, 512));
        order.setStatusReason(StringUtils.left(reason, 512));
        order.setMerchantStatusCode(MerchantOrderStatusEnum.FAILED.code());
        order.setMerchantStatusReason(MerchantOrderStatusEnum.FAILED.statusReason());
        order.setFailedAt(Instant.now());
        // 明确提交失败且不再切路由时释放冻结资金；UNKNOWN 不释放，等待查单确认。
        releasePayout(order);
        payoutOrderDao.updateById(order);
        recordStatusChange(order, fromStatus, order.getStatus(), "PSP_SUBMIT_FAILED", order.getStatusReason(), context);
        return SubmitDecision.done();
    }

    private void markSubmitUnknown(Long orderId, Exception ex, SubmitContext context) {
        markSubmitUnknown(orderId, null, ex, context);
    }

    /**
     * PSP 调用异常或系统异常时，订单进入 UNKNOWN，后续依赖查单确认最终结果。
     */
    private void markSubmitUnknown(Long orderId, PspRouteResult route, Exception ex, SubmitContext context) {
        transactionTemplate.executeWithoutResult(status -> {
            PayoutOrderEntity order = lockOrder(orderId);
            if (isTerminalStatus(order)) {
                return;
            }
            payoutRouteAttemptService.recordAttempt(order, route, unknownResult(ex));
            applySubmitUnknown(order,
                    ex instanceof PaymentException apiException ? apiException.getErrorCode().name() : ex.getClass().getSimpleName(),
                    ex.getMessage(),
                    context);
        });
    }

    /**
     * 提交 PSP 前冻结商户代付资金。
     * <p>
     * 如果订单已经冻结或已经进入无需提交的状态，会直接返回当前订单，保证重复触发时尽量幂等。
     */
    private PayoutOrderEntity freezeIfNeeded(PayoutOrderEntity order, SubmitContext context) {
        if (order == null || order.getId() == null) {
            throw new PaymentException(PaymentErrorCode.INVALID_REQUEST, "Payout order is required");
        }
        SubmitContext safeContext = context == null ? SubmitContext.system(order.getAppId(), null) : context;
        return transactionTemplate.execute(status -> {
            PayoutOrderEntity current = lockOrder(order.getId());
            if (shouldSkipPspSubmit(current) || StringUtils.isNotBlank(current.getHoldNo())) {
                return current;
            }
            try {
                LedgerPostingResult result = ledgerPostingService.freezePayout(payoutPostingRequest(current));
                payoutOrderStateService.markFrozen(current, result, stateContext("PAYOUT_FROZEN", null, safeContext));
                return current;
            } catch (InsufficientLedgerBalanceException ex) {
                markFreezeFailed(current, PaymentErrorCode.INSUFFICIENT_BALANCE.getMessage(),
                        PaymentErrorCode.INSUFFICIENT_BALANCE.name(), safeContext);
                if (safeContext.throwOnFreezeFailure()) {
                    throw new PaymentException(PaymentErrorCode.INSUFFICIENT_BALANCE, ex);
                }
                return current;
            } catch (PaymentException ex) {
                throw ex;
            } catch (Exception ex) {
                throw new PaymentException(PaymentErrorCode.SYSTEM_ERROR, ex);
            }
        });
    }

    private PayoutOrderEntity claimForSubmit(Long orderId, SubmitContext context) {
        return transactionTemplate.execute(status -> {
            PayoutOrderEntity order = lockOrder(orderId);
            if (!PayoutOrderStatusEnum.FROZEN.code().equals(order.getStatus())
                    || StringUtils.isBlank(order.getHoldNo())) {
                return null;
            }
            String fromStatus = order.getStatus();
            order.setStatus(PayoutOrderStatusEnum.PROCESSING.code());
            order.setPspStatus(PayoutOrderStatusEnum.PROCESSING.code());
            order.setStatusReason(PSP_SUBMITTING_REASON);
            order.setMerchantStatusCode(MerchantOrderStatusEnum.PROCESSING.code());
            order.setMerchantStatusReason(MerchantOrderStatusEnum.PROCESSING.statusReason());
            order.setSubmittedAt(Instant.now());
            order.setNextQueryAt(Instant.now().plusSeconds(firstQueryDelaySeconds()));
            payoutOrderDao.updateById(order);
            recordStatusChange(order, fromStatus, order.getStatus(), "PSP_SUBMITTING", PSP_SUBMITTING_REASON, context);
            return order;
        });
    }

    /**
     * 冻结失败时将订单打到失败状态，并记录状态变更。
     */
    private void markFreezeFailed(PayoutOrderEntity order, String reason, String failCode, SubmitContext context) {
        String message = StringUtils.defaultIfBlank(reason, "Payout freeze failed");
        payoutOrderStateService.markFreezeFailed(order, message, failCode,
                stateContext("PAYOUT_FREEZE_FAILED", message, context));
    }

    private void applySubmitUnknown(PayoutOrderEntity order,
                                    PspPayoutDispatchResult result,
                                    SubmitContext context) {
        applySubmitUnknown(order,
                StringUtils.defaultIfBlank(result.getErrorCode(), result.getResponseCode()),
                StringUtils.defaultIfBlank(result.getErrorMessage(), result.getResponseMessage()),
                context);
    }

    /**
     * 标记 PSP 提交结果未知。
     * <p>
     * UNKNOWN 不释放冻结资金，避免 PSP 实际已受理但系统提前退款给商户。
     */
    private void applySubmitUnknown(PayoutOrderEntity order,
                                    String failCode,
                                    String failMessage,
                                    SubmitContext context) {
        payoutOrderStateService.markSubmitUnknown(
                order,
                failCode,
                failMessage,
                PSP_SUBMIT_UNKNOWN_REASON,
                Instant.now().plusSeconds(firstQueryDelaySeconds()),
                stateContext("PSP_SUBMIT_UNKNOWN", PSP_SUBMIT_UNKNOWN_REASON, context)
        );
    }

    /**
     * 使用 for update 锁定订单，保证提交结果、路由切换、资金释放等状态变更串行执行。
     */
    private PayoutOrderEntity lockOrder(Long orderId) {
        PayoutOrderEntity order = payoutOrderDao.selectOne(new QueryWrapper<PayoutOrderEntity>()
                .eq("id", orderId)
                .last("limit 1 for update"));
        if (order == null) {
            throw new PaymentException(PaymentErrorCode.INVALID_REQUEST, "Payout order not found");
        }
        return order;
    }

    /**
     * 订单是否已经进入不可再变更的终态。
     */
    private boolean isTerminalStatus(PayoutOrderEntity order) {
        return PayoutOrderStatusEnum.SUCCESS.code().equals(order.getStatus())
                || PayoutOrderStatusEnum.FAILED.code().equals(order.getStatus())
                || PayoutOrderStatusEnum.CANCELLED.code().equals(order.getStatus());
    }

    /**
     * 判断是否应该跳过 PSP 提交，避免重复提交已经处理中、人工审核或终态订单。
     */
    private boolean shouldSkipPspSubmit(PayoutOrderEntity order) {
        return PayoutOrderStatusEnum.PROCESSING.code().equals(order.getStatus())
                || PayoutOrderStatusEnum.MANUAL_REVIEW.code().equals(order.getStatus())
                || isTerminalStatus(order);
    }

    private boolean shouldStopSubmitFlow(PayoutOrderEntity order) {
        return PayoutOrderStatusEnum.MANUAL_REVIEW.code().equals(order.getStatus())
                || isTerminalStatus(order);
    }

    /**
     * 释放代付冻结资金。
     * <p>
     * 只在明确失败场景调用；释放失败只记录日志，不覆盖订单原始失败原因。
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
            throw new PaymentException(PaymentErrorCode.SYSTEM_ERROR, ex);
        }
    }

    /**
     * 基于当前订单和禁用集合重新解析下一条可用代付路由。
     */
    private PayoutPlan resolveNextPlan(PayoutOrderEntity order,
                                       Set<Long> disabledPspIds,
                                       Set<Long> disabledAccountIds,
                                       Set<Long> disabledRouteOptionIds) {
        try {
            return payoutPlanService.resolve(order, disabledPspIds, disabledAccountIds, disabledRouteOptionIds);
        } catch (PaymentException ex) {
            if (PaymentErrorCode.UNSUPPORTED_METHOD.equals(ex.getErrorCode())) {
                return null;
            }
            throw ex;
        }
    }

    /**
     * 将订单切换到新的 PSP 路由，并清理上一轮 PSP 提交结果字段。
     */
    private PspRouteResult switchRoute(Long orderId, PayoutPlan plan, SubmitContext context) {
        if (plan == null || plan.getRoute() == null) {
            return null;
        }
        PayoutOrderEntity order = lockOrder(orderId);
        if (shouldStopSubmitFlow(order)) {
            return null;
        }
        String fromStatus = order.getStatus();
        applyPayoutRoutePlan(order, plan);
        order.setStatusReason(ROUTE_SWITCH_REASON);
        order.setMerchantStatusCode(MerchantOrderStatusEnum.PROCESSING.code());
        order.setMerchantStatusReason(MerchantOrderStatusEnum.PROCESSING.statusReason());
        payoutOrderDao.updateById(order);
        recordStatusChange(order, fromStatus, order.getStatus(), "PAYOUT_ROUTE_SWITCH", ROUTE_SWITCH_REASON, context);
        return plan.getRoute();
    }

    /**
     * 将新的 payment plan 路由快照写回代付订单。
     */
    private void applyPayoutRoutePlan(PayoutOrderEntity order, PayoutPlan plan) {
        PspRouteResult route = plan.getRoute();
        order.setPaymentPlanCatalogId(plan.getCatalogId());
        order.setPaymentPlanVersion(plan.getCatalogVersion());
        order.setPaymentPlanBucketId(plan.getBucketId());
        order.setPaymentPlanRouteOptionId(plan.getRouteOptionId());
        applyRoute(order, route);
        order.setPspFeeAmount(defaultZero(plan.getPspFeeAmount()));
        if (plan.getPspFee() != null && plan.getPspFee().getRule() != null) {
            order.setPspFeeRuleId(plan.getPspFee().getRule().getId());
            order.setPspFeeSnapshotJson(plan.getPspFee().getSnapshotJson());
        } else {
            order.setPspFeeRuleId(null);
            order.setPspFeeSnapshotJson(null);
        }
        order.setPspRequestNo(null);
        order.setPspOrderNo(null);
        order.setPspStatus(null);
        order.setPspRawStatus(null);
        order.setNextQueryAt(null);
    }

    /**
     * 写入订单当前使用的 PSP 路由字段。
     */
    private void applyRoute(PayoutOrderEntity order, PspRouteResult route) {
        order.setRouteRuleId(route.getRouteRuleId());
        order.setRouteGroupId(route.getRouteGroupId());
        order.setRouteChannelId(route.getRouteChannelId());
        order.setPspId(route.getPspId());
        order.setPspCode(route.getPspCode());
        order.setPspMethodId(route.getPspMethodId());
        order.setPspMethodCode(route.getPspMethodCode());
        order.setPspAccountId(route.getPspAccountId());
        order.setPspAccountNo(route.getPspAccountNo());
        order.setRouteSnapshotJson(routeSnapshotJson(order, route));
    }

    /**
     * 保存订单路由快照，方便后续排查当时选择的 plan、route、PSP 账号等信息。
     */
    private String routeSnapshotJson(PayoutOrderEntity order, PspRouteResult route) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("catalogId", order.getPaymentPlanCatalogId());
        snapshot.put("catalogVersion", order.getPaymentPlanVersion());
        snapshot.put("bucketId", order.getPaymentPlanBucketId());
        snapshot.put("routeOptionId", order.getPaymentPlanRouteOptionId());
        snapshot.put("routeRuleId", route.getRouteRuleId());
        snapshot.put("routeGroupId", route.getRouteGroupId());
        snapshot.put("routeChannelId", route.getRouteChannelId());
        snapshot.put("pspId", route.getPspId());
        snapshot.put("pspCode", route.getPspCode());
        snapshot.put("pspMethodId", route.getPspMethodId());
        snapshot.put("pspMethodCode", route.getPspMethodCode());
        snapshot.put("pspAccountId", route.getPspAccountId());
        snapshot.put("pspAccountNo", route.getPspAccountNo());
        snapshot.put("pspBankCode", route.getPspBankCode());
        return JSON.toJSONString(snapshot, JSONWriter.Feature.WriteMapNullValue);
    }

    /**
     * 多次切换后仍没有可用路由时，将订单最终置为失败并释放冻结资金。
     */
    private void markRouteUnavailableFailed(Long orderId, PspPayoutDispatchResult result, SubmitContext context) {
        transactionTemplate.executeWithoutResult(status -> {
            PayoutOrderEntity order = lockOrder(orderId);
            if (isTerminalStatus(order)) {
                return;
            }
            String fromStatus = order.getStatus();
            String reason = StringUtils.defaultIfBlank(
                    result == null ? null : result.getErrorMessage(),
                    StringUtils.defaultIfBlank(result == null ? null : result.getResponseMessage(), ROUTE_UNAVAILABLE_FAILED_REASON)
            );
            order.setPspRequestNo(result == null ? order.getPspRequestNo() : result.getPspRequestNo());
            order.setPspOrderNo(result == null ? order.getPspOrderNo() : result.getPspOrderNo());
            order.setPspRawStatus(result == null ? order.getPspRawStatus() : result.getRawStatus());
            order.setStatus(PayoutOrderStatusEnum.FAILED.code());
            order.setPspStatus(PayoutOrderStatusEnum.FAILED.code());
            order.setFailCode(StringUtils.defaultIfBlank(
                    result == null ? null : result.getErrorCode(),
                    "PAYOUT_ROUTE_UNAVAILABLE"
            ));
            order.setFailMsg(StringUtils.left(reason, 512));
            order.setStatusReason(StringUtils.left(reason, 512));
            order.setMerchantStatusCode(MerchantOrderStatusEnum.FAILED.code());
            order.setMerchantStatusReason(MerchantOrderStatusEnum.routeUnavailableReason());
            order.setFailedAt(Instant.now());
            // 已无可用路由，代付不会继续提交 PSP，此时可以释放商户冻结资金。
            releasePayout(order);
            payoutOrderDao.updateById(order);
            recordStatusChange(order, fromStatus, order.getStatus(),
                    "PAYOUT_ROUTE_UNAVAILABLE", order.getStatusReason(), context);
        });
    }

    /**
     * 将异常包装成 UNKNOWN 提交结果，用于统一记录 attempt 和订单状态。
     */
    private PspPayoutDispatchResult unknownResult(Exception ex) {
        PspPayoutDispatchResult result = new PspPayoutDispatchResult();
        result.setSuccess(false);
        result.setSubmitResultStatus(PspPayoutSubmitStatus.UNKNOWN);
        result.setErrorCode(ex instanceof PaymentException apiException
                ? apiException.getErrorCode().name()
                : ex.getClass().getSimpleName());
        result.setErrorMessage(ex.getMessage());
        return result;
    }

    /**
     * 构建冻结或释放代付资金所需的账务请求。
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
     * PSP 受理或 UNKNOWN 后首次查单延迟。
     */
    private long firstQueryDelaySeconds() {
        long seconds = configService.payoutSubmit().getFirstQueryDelaySeconds();
        return seconds <= 0 ? 60L : seconds;
    }

    /**
     * 可换路由场景下最多尝试的提交次数，增加上限保护避免配置错误造成长循环。
     */
    private int maxRouteAttempts() {
        int attempts = configService.payoutSubmit().getMaxRouteAttempts();
        if (attempts <= 0) {
            attempts = DEFAULT_MAX_ROUTE_ATTEMPTS;
        }
        return Math.min(attempts, MAX_ROUTE_ATTEMPTS_LIMIT);
    }

    /**
     * 将非空 ID 加入禁用集合。
     */
    private void addIfNotNull(Set<Long> values, Long value) {
        if (value != null) {
            values.add(value);
        }
    }

    /**
     * 金额空值按 0 处理，避免写入订单时出现 NPE。
     */
    private BigDecimal defaultZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    /**
     * 统一记录代付订单状态变更。
     */
    private void recordStatusChange(PayoutOrderEntity order,
                                    String fromStatus,
                                    String toStatus,
                                    String eventType,
                                    String reason,
                                    SubmitContext context) {
        payoutOrderStateService.recordChange(order, fromStatus, toStatus, stateContext(eventType, reason, context));
    }

    /**
     * 构建订单状态变更上下文。
     */
    private OrderStateChangeContext stateContext(String eventType, String reason, SubmitContext context) {
        SubmitContext safeContext = context == null ? SubmitContext.system(null, null) : context;
        return new OrderStateChangeContext(
                eventType,
                reason,
                safeContext.operatorType(),
                safeContext.appId(),
                null,
                safeContext.traceId()
        );
    }

    /**
     * PSP 提交处理后的内部决策。
     * <p>
     * routeUnavailable=true 表示当前 PSP 路由可跳过并继续尝试下一条路由。
     */
    private record SubmitDecision(boolean routeUnavailable, Long pspAccountId, Long routeOptionId) {
        private static SubmitDecision done() {
            return new SubmitDecision(false, null, null);
        }

        private static SubmitDecision routeUnavailable(Long pspAccountId, Long routeOptionId) {
            return new SubmitDecision(true, pspAccountId, routeOptionId);
        }
    }

    /**
     * 代付提交上下文。
     * <p>
     * operatorType/appId/traceId 用于状态日志；throwOnFreezeFailure 控制冻结失败时是否向上抛异常。
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
