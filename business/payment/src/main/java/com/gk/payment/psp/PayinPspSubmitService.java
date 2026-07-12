package com.gk.payment.psp;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.gk.payment.domain.enums.PayDirectionEnum;
import com.gk.payment.config.PaymentConfigService;
import com.gk.infra.utils.AsynUtils;
import com.gk.payment.domain.error.PaymentErrorCode;
import com.gk.payment.domain.error.PaymentException;
import com.gk.payment.dao.PayinOrderDao;
import com.gk.payment.entity.PayinOrderEntity;
import com.gk.payment.enums.MerchantOrderStatusEnum;
import com.gk.payment.enums.PayinOrderStatusEnum;
import com.gk.payment.plan.model.PayinPlan;
import com.gk.payment.service.OrderStatusLogService;
import com.gk.psp.dispatch.PspPayDispatchResult;
import com.gk.psp.dispatch.PspPayDispatchService;
import com.gk.psp.route.PspRouteResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayinPspSubmitService {
    private final PayinOrderDao payinOrderDao;
    private final PspPayDispatchService pspPayDispatchService;
    private final PaymentConfigService configService;
    private final OrderStatusLogService orderStatusLogService;

    public PayinOrderEntity submit(PayinOrderEntity order, PayinPlan payinPlan, SubmitContext context) {
        if (order == null || order.getId() == null) {
            throw new PaymentException(PaymentErrorCode.INVALID_REQUEST, "Payin order is required");
        }
        SubmitContext safeContext = context == null ? SubmitContext.system(order.getAppId(), null) : context;
        if (payinPlan == null || payinPlan.getRoute() == null) {
            PaymentException ex = new PaymentException(PaymentErrorCode.SERVICE_NOT_READY, "Payin plan is not resolved");
            handleSubmitException(order, ex, safeContext);
            throw ex;
        }
        return submit(order, payinPlan.getRoute(), safeContext);
    }

    public PayinOrderEntity submit(PayinOrderEntity order, PspRouteResult route, SubmitContext context) {
        if (order == null || order.getId() == null) {
            throw new PaymentException(PaymentErrorCode.INVALID_REQUEST, "Payin order is required");
        }
        SubmitContext safeContext = context == null ? SubmitContext.system(order.getAppId(), null) : context;
        if (route == null) {
            PaymentException ex = new PaymentException(PaymentErrorCode.SERVICE_NOT_READY, "PSP route is unavailable");
            handleSubmitException(order, ex, safeContext);
            throw ex;
        }

        PayinOrderEntity submitOrder = submitOrder(order, safeContext);
        if (submitOrder == null) {
            return payinOrderDao.selectById(order.getId());
        }

        try {
            PspPayDispatchResult result = pspPayDispatchService.dispatch(PspOrderRequests.fromPayinOrder(submitOrder), route);
            return applyDispatchResult(submitOrder, result, safeContext);
        } catch (PaymentException ex) {
            handleSubmitException(submitOrder, ex, safeContext);
            throw ex;
        } catch (Exception ex) {
            log.error("Payin PSP submit failed, payinOrderNo={}, merchantOrderNo={}",
                    submitOrder.getPayinOrderNo(), submitOrder.getMerchantOrderNo(), ex);
            PaymentException apiException = new PaymentException(PaymentErrorCode.SYSTEM_ERROR, ex);
            handleSubmitException(submitOrder, apiException, safeContext);
            throw apiException;
        }
    }

    private PayinOrderEntity applyDispatchResult(PayinOrderEntity order,
                                                 PspPayDispatchResult result,
                                                 SubmitContext context) {
        if (result == null) {
            throw new PaymentException(PaymentErrorCode.SYSTEM_ERROR, "PSP payin submit result is empty");
        }
        String fromStatus = order.getStatus();
        String toStatus = result.isSuccess() ? PayinOrderStatusEnum.PROCESSING.code() : PayinOrderStatusEnum.FAILED.code();
        Instant now = Instant.now();

        UpdateWrapper<PayinOrderEntity> wrapper = new UpdateWrapper<>();
        wrapper.eq("id", order.getId())
                .eq("status", PayinOrderStatusEnum.CREATED.code())
                .set("psp_request_no", result.getPspRequestNo())
                .set("psp_order_no", result.getPspOrderNo())
                .set("psp_pay_url", result.getPayUrl())
                .set("psp_pay_params_json", result.getPayParamsJson())
                .set("psp_raw_status", result.getRawStatus())
                .set("status", toStatus)
                .set("psp_status", toStatus);

        String reason = null;
        if (result.isSuccess()) {
            wrapper.set("merchant_status_code", MerchantOrderStatusEnum.PROCESSING.code())
                    .set("merchant_status_reason", MerchantOrderStatusEnum.PROCESSING.statusReason())
                    .set("submitted_at", now)
                    .set("next_query_at", now.plusSeconds(firstQueryDelaySeconds()));
        } else {
            reason = StringUtils.defaultIfBlank(
                    result.getErrorMessage(),
                    StringUtils.defaultIfBlank(result.getResponseMessage(), "PSP submit failed")
            );
            wrapper.set("status_reason", StringUtils.left(reason, 512))
                    .set("merchant_status_code", MerchantOrderStatusEnum.FAILED.code())
                    .set("merchant_status_reason", MerchantOrderStatusEnum.FAILED.statusReason())
                    .set("failed_at", now)
                    .set("next_query_at", null);
        }

        if (payinOrderDao.update(null, wrapper) > 0) {
            recordStatusChange(order, fromStatus, toStatus,
                    result.isSuccess() ? "PSP_SUBMIT" : "PSP_SUBMIT_FAILED",
                    reason,
                    context);
        }
        return payinOrderDao.selectById(order.getId());
    }

    private PayinOrderEntity submitOrder(PayinOrderEntity order, SubmitContext context) {
        if (context != null && context.markFailedOnException()) {
            return order;
        }
        PayinOrderEntity currentOrder = payinOrderDao.selectById(order.getId());
        if (currentOrder == null) {
            throw new PaymentException(PaymentErrorCode.INVALID_REQUEST, "Payin order not found");
        }
        return shouldSkipPspSubmit(currentOrder) ? null : currentOrder;
    }

    private boolean shouldSkipPspSubmit(PayinOrderEntity order) {
        return PayinOrderStatusEnum.PROCESSING.code().equals(order.getStatus())
                || PayinOrderStatusEnum.MANUAL_REVIEW.code().equals(order.getStatus())
                || PayinOrderStatusEnum.SUCCESS.code().equals(order.getStatus())
                || PayinOrderStatusEnum.FAILED.code().equals(order.getStatus())
                || PayinOrderStatusEnum.CLOSED.code().equals(order.getStatus());
    }

    private void handleSubmitException(PayinOrderEntity order, PaymentException ex, SubmitContext context) {
        if (order != null && context != null && context.markFailedOnException()) {
            markFailed(order, ex.getMessage(), context);
        }
    }

    private void markFailed(PayinOrderEntity order, String reason, SubmitContext context) {
        String fromStatus = order.getStatus();
        String message = StringUtils.defaultIfBlank(reason, "Pay order failed");
        UpdateWrapper<PayinOrderEntity> wrapper = new UpdateWrapper<>();
        wrapper.eq("id", order.getId())
                .eq("status", PayinOrderStatusEnum.CREATED.code())
                .set("status", PayinOrderStatusEnum.FAILED.code())
                .set("status_reason", StringUtils.left(message, 512))
                .set("merchant_status_code", MerchantOrderStatusEnum.FAILED.code())
                .set("merchant_status_reason", MerchantOrderStatusEnum.FAILED.statusReason())
                .set("failed_at", Instant.now())
                .set("next_query_at", null);
        if (payinOrderDao.update(null, wrapper) > 0) {
            recordStatusChange(order, fromStatus, PayinOrderStatusEnum.FAILED.code(),
                    "ORDER_FAILED", message, context);
        }
    }

    private void recordStatusChange(PayinOrderEntity order,
                                    String fromStatus,
                                    String toStatus,
                                    String eventType,
                                    String reason,
                                    SubmitContext context) {
        SubmitContext safeContext = context == null ? SubmitContext.system(order.getAppId(), null) : context;
        AsynUtils.execute("Order status log", () -> orderStatusLogService.recordChange(
                PayDirectionEnum.PAYIN.code(),
                order.getTenantId(),
                order.getMerchantId(),
                order.getId(),
                order.getPayinOrderNo(),
                fromStatus,
                toStatus,
                eventType,
                reason,
                safeContext.operatorType(),
                safeContext.appId(),
                order.getMerchantOrderNo(),
                safeContext.traceId()
        ));
    }

    private long firstQueryDelaySeconds() {
        List<Long> backoffSeconds = configService.pspQuery().getBackoffSeconds();
        if (backoffSeconds == null || backoffSeconds.isEmpty()) {
            return 60L;
        }
        return Math.max(1L, backoffSeconds.getFirst());
    }

    public record SubmitContext(String operatorType, String appId, String traceId, boolean markFailedOnException) {
        public static SubmitContext merchant(String appId, String traceId) {
            return new SubmitContext("MERCHANT", appId, traceId, true);
        }

        public static SubmitContext system(String appId, String traceId) {
            return new SubmitContext("SYSTEM", appId, traceId, false);
        }
    }
}
