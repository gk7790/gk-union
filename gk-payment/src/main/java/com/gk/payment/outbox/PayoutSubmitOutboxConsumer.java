package com.gk.payment.outbox;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gk.infra.config.service.GkSysParamsConfigService;
import com.gk.ledger.exception.InsufficientLedgerBalanceException;
import com.gk.ledger.posting.LedgerPostingResult;
import com.gk.ledger.posting.PayoutPostingRequest;
import com.gk.ledger.service.LedgerPostingService;
import com.gk.infra.utils.AsynUtils;
import com.gk.openapi.error.ApiErrorCode;
import com.gk.openapi.error.ApiException;
import com.gk.payment.dao.PayoutOrderDao;
import com.gk.payment.entity.PayoutOrderEntity;
import com.gk.payment.enums.PayoutOrderStatusEnum;
import com.gk.psp.dao.PspAccountDao;
import com.gk.psp.dao.PspMethodDao;
import com.gk.psp.dao.PspProviderDao;
import com.gk.payment.psp.PspOrderRequests;
import com.gk.payment.service.OrderStatusLogService;
import com.gk.psp.callback.support.PspCallbackUrlBuilder;
import com.gk.psp.entity.PspAccountEntity;
import com.gk.psp.entity.PspMethodEntity;
import com.gk.psp.entity.PspProviderEntity;
import com.gk.psp.dispatch.PspPayoutDispatchResult;
import com.gk.psp.dispatch.PspPayoutDispatchService;
import com.gk.psp.route.PspRouteResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * 代付提交 outbox 业务消费者 * <p>
 * 本类负责执行一{@link PayoutSubmitOutboxPayload} 对应的真实业务动作：
 * 读取代付订单、必要时冻结商户余额、按订单上已固化PSP 路由快照提交 PSP * 并推进订单状态。重试、死信和 outbox 状态更新由 {@link PayoutSubmitOutboxTask} 负责 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PayoutSubmitOutboxConsumer {
    private final PayoutOrderDao payoutOrderDao;
    private final LedgerPostingService ledgerPostingService;
    private final PspPayoutDispatchService pspPayoutDispatchService;
    private final PspProviderDao pspProviderDao;
    private final PspAccountDao pspAccountDao;
    private final PspMethodDao pspMethodDao;
    private final PspCallbackUrlBuilder callbackUrlBuilder;
    private final OrderStatusLogService orderStatusLogService;
    private final GkSysParamsConfigService configService;

    /**
     * 消费单条 outbox payload     * <p>
     * 方法具备业务幂等：如果订单已经进PROCESSING/SUCCESS/FAILED/CANCELLED     * 说明已提交或已终结，直接返回，让 outbox 事件可以被标记为完成     */
    public void consume(String payloadJson) {
        PayoutSubmitOutboxPayload payload = JSON.parseObject(payloadJson, PayoutSubmitOutboxPayload.class);
        if (payload == null || StringUtils.isBlank(payload.payoutOrderNo())) {
            throw new ApiException(ApiErrorCode.INVALID_REQUEST, "Invalid payout submit outbox payload");
        }
        PayoutOrderEntity order = payoutOrderDao.selectOne(new QueryWrapper<PayoutOrderEntity>()
                .eq("tenant_id", payload.tenantId())
                .eq("payout_order_no", payload.payoutOrderNo())
                .last("limit 1"));
        if (order == null) {
            throw new ApiException(ApiErrorCode.INVALID_REQUEST, "Payout order not found: " + payload.payoutOrderNo());
        }
        if (isTerminalOrAlreadySubmitted(order)) {
            return;
        }
        freezeIfNeeded(order);
        submitToPsp(order);
    }

    /**
     * 判断订单是否已经无需再次提交 PSP     */
    private boolean isTerminalOrAlreadySubmitted(PayoutOrderEntity order) {
        return PayoutOrderStatusEnum.PROCESSING.code().equals(order.getStatus())
                || PayoutOrderStatusEnum.SUCCESS.code().equals(order.getStatus())
                || PayoutOrderStatusEnum.FAILED.code().equals(order.getStatus())
                || PayoutOrderStatusEnum.CANCELLED.code().equals(order.getStatus());
    }

    /**
     * 在提PSP 前冻结商户可用余额     * <p>
     * 如果订单已有 holdNo，说明此前已经冻结成功，重试时不再重复冻结     * 如果冻结成功但更新订单失败，会尝试释放冻结资金，避免资金长期卡在冻结户     */
    private void freezeIfNeeded(PayoutOrderEntity order) {
        if (StringUtils.isNotBlank(order.getHoldNo())) {
            return;
        }
        boolean frozen = false;
        try {
            LedgerPostingResult result = ledgerPostingService.freezePayout(payoutPostingRequest(order));
            frozen = true;
            order.setHoldNo(result.getHoldNo());
            order.setFreezeJournalNo(result.getJournalNo());
            order.setStatus(PayoutOrderStatusEnum.FROZEN.code());
            order.setStatusReason(null);
            payoutOrderDao.updateById(order);
            recordStatusChange(order, PayoutOrderStatusEnum.CREATED.code(), order.getStatus(), "PAYOUT_FROZEN", null);
        } catch (InsufficientLedgerBalanceException ex) {
            markFailed(order, ApiErrorCode.INSUFFICIENT_BALANCE.getMessage(), ApiErrorCode.INSUFFICIENT_BALANCE.name());
            throw new ApiException(ApiErrorCode.INSUFFICIENT_BALANCE, ex);
        } catch (Exception ex) {
            if (frozen) {
                releasePayout(order);
            }
            markFailed(order, ApiErrorCode.SYSTEM_ERROR.getMessage(), ApiErrorCode.SYSTEM_ERROR.name());
            throw new ApiException(ApiErrorCode.SYSTEM_ERROR, ex);
        }
    }

    /**
     * 使用订单保存的路由快照提PSP     * <p>
     * 这里不重新解析支付方案，避免异步消费时后台配置变化导致路由、账户或费率与下单时不一致     */
    private void submitToPsp(PayoutOrderEntity order) {
        try {
            PspRouteResult route = routeSnapshot(order);
            PspPayoutDispatchResult dispatchResult = pspPayoutDispatchService.dispatch(PspOrderRequests.fromPayoutOrder(order), route);
            applyDispatchResult(order, dispatchResult);
            if (!dispatchResult.isSuccess()) {
                releasePayout(order);
            }
            payoutOrderDao.updateById(order);
        } catch (ApiException ex) {
            releasePayout(order);
            markFailed(order, ex.getMessage(), ex.getErrorCode().name());
            throw ex;
        } catch (Exception ex) {
            log.error("Payout outbox PSP submit failed, payoutOrderNo={}", order.getPayoutOrderNo(), ex);
            releasePayout(order);
            markFailed(order, ApiErrorCode.SYSTEM_ERROR.getMessage(), ApiErrorCode.SYSTEM_ERROR.name());
            throw new ApiException(ApiErrorCode.SYSTEM_ERROR, ex);
        }
    }

    /**
     * 从订单固化字段和当前 PSP 资源表重建提交所需的路由对象     * <p>
     * 订单保存 PSP/路由 ID 和快照字段；密钥、baseUrl、配置等敏感信息只从 PSP 资源表读取，
     * 不进outbox payload 或订单快照     */
    private PspRouteResult routeSnapshot(PayoutOrderEntity order) {
        if (order.getPspId() == null || order.getPspAccountId() == null || StringUtils.isBlank(order.getPspCode())) {
            throw new ApiException(ApiErrorCode.INVALID_REQUEST, "PSP route snapshot is incomplete");
        }
        PspProviderEntity provider = pspProviderDao.selectById(order.getPspId());
        PspAccountEntity account = pspAccountDao.selectById(order.getPspAccountId());
        PspMethodEntity method = order.getPspMethodId() == null ? null : pspMethodDao.selectById(order.getPspMethodId());
        if (provider == null || account == null) {
            throw new ApiException(ApiErrorCode.SERVICE_NOT_READY, "PSP route snapshot is unavailable");
        }

        PspRouteResult route = new PspRouteResult();
        route.setRouteRuleId(order.getRouteRuleId());
        route.setRouteGroupId(order.getRouteGroupId());
        route.setRouteChannelId(order.getRouteChannelId());
        route.setPspId(order.getPspId());
        route.setPspCode(order.getPspCode());
        route.setPspBaseUrl(provider.getBaseUrl());
        route.setProviderConfigJson(provider.getConfigJson());
        route.setPspCallbackUrl(callbackUrlBuilder.payoutCallbackUrl(order.getPspAccountNo()));
        route.setPspMethodId(order.getPspMethodId());
        route.setPspMethodCode(order.getPspMethodCode());
        route.setMethodConfigJson(method == null ? null : method.getConfigJson());
        route.setPspAccountId(order.getPspAccountId());
        route.setPspAccountNo(order.getPspAccountNo());
        route.setPspAccountApiKey(account.getApiKey());
        route.setPspAccountApiSecret(account.getApiSecret());
        route.setAccountConfigJson(account.getConfigJson());
        route.setPspBankCode(pspBankCode(order.getRouteSnapshotJson()));
        return route;
    }

    /**
     * 从订单路由快JSON 中读取非敏感路由字段，例PSP 侧银行编码     */
    private String pspBankCode(String routeSnapshotJson) {
        if (StringUtils.isBlank(routeSnapshotJson)) {
            return null;
        }
        try {
            JSONObject snapshot = JSON.parseObject(routeSnapshotJson);
            return snapshot == null ? null : StringUtils.trimToNull(snapshot.getString("pspBankCode"));
        } catch (Exception ex) {
            log.warn("Parse payout route snapshot failed, key=pspBankCode, err={}", ex.getMessage());
            return null;
        }
    }

    /**
     * PSP 提交结果回写到代付订单     */
    private void applyDispatchResult(PayoutOrderEntity order, PspPayoutDispatchResult result) {
        String fromStatus = order.getStatus();
        order.setPspRequestNo(result.getPspRequestNo());
        order.setPspOrderNo(result.getPspOrderNo());
        order.setPspRawStatus(result.getRawStatus());
        if (result.isSuccess()) {
            order.setStatus(PayoutOrderStatusEnum.PROCESSING.code());
            order.setPspStatus(PayoutOrderStatusEnum.PROCESSING.code());
            order.setSubmittedAt(Instant.now());
            order.setNextQueryAt(Instant.now().plusSeconds(firstQueryDelaySeconds()));
            recordStatusChange(order, fromStatus, order.getStatus(), "PSP_SUBMIT", null);
            return;
        }
        order.setStatus(PayoutOrderStatusEnum.FAILED.code());
        order.setPspStatus(PayoutOrderStatusEnum.FAILED.code());
        order.setFailCode(result.getErrorCode());
        order.setFailMsg(StringUtils.left(result.getErrorMessage(), 512));
        order.setStatusReason(StringUtils.defaultIfBlank(result.getErrorMessage(), "PSP payout submit failed"));
        order.setFailedAt(Instant.now());
        recordStatusChange(order, fromStatus, order.getStatus(), "PSP_SUBMIT_FAILED", order.getStatusReason());
    }

    /**
     * 将订单标记为失败并记录状态变更     */
    private void markFailed(PayoutOrderEntity order, String reason, String failCode) {
        String fromStatus = order.getStatus();
        order.setStatus(PayoutOrderStatusEnum.FAILED.code());
        String message = StringUtils.defaultIfBlank(reason, "Payout order failed");
        order.setStatusReason(message);
        order.setFailCode(failCode);
        order.setFailMsg(StringUtils.left(reason, 512));
        order.setFailedAt(Instant.now());
        payoutOrderDao.updateById(order);
        recordStatusChange(order, fromStatus, order.getStatus(), "ORDER_FAILED", message);
    }

    /**
     * PSP 提交失败或消费异常时释放已冻结资金     * <p>
     * 已释放过的订单通过 releaseJournalNo 跳过，保证重试路径不会重复释放     */
    private void releasePayout(PayoutOrderEntity order) {
        if (StringUtils.isBlank(order.getHoldNo()) || StringUtils.isNotBlank(order.getReleaseJournalNo())) {
            return;
        }
        try {
            LedgerPostingResult result = ledgerPostingService.releasePayout(payoutPostingRequest(order));
            order.setReleaseJournalNo(result.getJournalNo());
            payoutOrderDao.updateById(order);
        } catch (Exception ex) {
            log.warn("Release payout after outbox submit failure failed, payoutOrderNo={}, err={}",
                    order.getPayoutOrderNo(), ex.getMessage());
        }
    }

    /**
     * 构建账务冻结/释放请求，字段必须与订单落库快照保持一致     */
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

    private long firstQueryDelaySeconds() {
        long seconds = configService.payoutSubmitConfig().getFirstQueryDelaySeconds();
        return seconds <= 0 ? 60L : seconds;
    }

    /**
     * 异步记录订单状态日志，不阻outbox 主处理流程     */
    private void recordStatusChange(PayoutOrderEntity order,
                                    String fromStatus,
                                    String toStatus,
                                    String eventType,
                                    String reason) {
        AsynUtils.execute("Order status log", () -> orderStatusLogService.recordChange(
                "PAYOUT",
                order.getTenantId(),
                order.getMerchantId(),
                order.getId(),
                order.getPayoutOrderNo(),
                fromStatus,
                toStatus,
                eventType,
                reason,
                "SYSTEM",
                order.getAppId(),
                order.getMerchantOrderNo(),
                null
        ));
    }
}
