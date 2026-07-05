package com.gk.payment.outbox;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.gk.common.enums.PayDirectionEnum;
import com.gk.infra.config.service.GkSysParamsConfigService;
import com.gk.infra.utils.AsynUtils;
import com.gk.openapi.error.ApiErrorCode;
import com.gk.openapi.error.ApiException;
import com.gk.payment.dao.PayinOrderDao;
import com.gk.payment.entity.PayinOrderEntity;
import com.gk.payment.enums.MerchantOrderStatusEnum;
import com.gk.payment.enums.PayinOrderStatusEnum;
import com.gk.payment.psp.PspOrderRequests;
import com.gk.payment.service.OrderStatusLogService;
import com.gk.psp.callback.support.PspCallbackUrlBuilder;
import com.gk.psp.dao.PspAccountDao;
import com.gk.psp.dao.PspMethodDao;
import com.gk.psp.dao.PspProviderDao;
import com.gk.psp.dispatch.PspPayDispatchResult;
import com.gk.psp.dispatch.PspPayDispatchService;
import com.gk.psp.entity.PspAccountEntity;
import com.gk.psp.entity.PspMethodEntity;
import com.gk.psp.entity.PspProviderEntity;
import com.gk.psp.route.PspRouteResult;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PayinSubmitOutboxConsumer {
    private final PayinOrderDao payinOrderDao;
    private final PspProviderDao pspProviderDao;
    private final PspAccountDao pspAccountDao;
    private final PspMethodDao pspMethodDao;
    private final PspCallbackUrlBuilder callbackUrlBuilder;
    private final PspPayDispatchService pspPayDispatchService;
    private final GkSysParamsConfigService configService;
    private final OrderStatusLogService orderStatusLogService;

    public void consume(String payloadJson) {
        PayinSubmitOutboxPayload payload = JSON.parseObject(payloadJson, PayinSubmitOutboxPayload.class);
        if (payload == null || StringUtils.isBlank(payload.payinOrderNo())) {
            throw new ApiException(ApiErrorCode.INVALID_REQUEST, "Invalid payin submit outbox payload");
        }
        PayinOrderEntity order = payinOrderDao.selectOne(new QueryWrapper<PayinOrderEntity>()
                .eq("tenant_id", payload.tenantId())
                .eq("payin_order_no", payload.payinOrderNo())
                .last("limit 1"));
        if (order == null) {
            throw new ApiException(ApiErrorCode.INVALID_REQUEST, "Payin order not found: " + payload.payinOrderNo());
        }
        if (shouldSkipPspSubmit(order)) {
            return;
        }

        PspRouteResult route = buildPspSubmitRoute(order);
        PspPayDispatchResult result = pspPayDispatchService.dispatch(PspOrderRequests.fromPayinOrder(order), route);
        applyDispatchResult(order, result);
    }

    private boolean shouldSkipPspSubmit(PayinOrderEntity order) {
        return PayinOrderStatusEnum.PROCESSING.code().equals(order.getStatus())
                || PayinOrderStatusEnum.MANUAL_REVIEW.code().equals(order.getStatus())
                || PayinOrderStatusEnum.SUCCESS.code().equals(order.getStatus())
                || PayinOrderStatusEnum.FAILED.code().equals(order.getStatus())
                || PayinOrderStatusEnum.CLOSED.code().equals(order.getStatus());
    }

    private PspRouteResult buildPspSubmitRoute(PayinOrderEntity order) {
        if (order.getPspId() == null || order.getPspAccountId() == null || StringUtils.isBlank(order.getPspCode())) {
            throw new ApiException(ApiErrorCode.SERVICE_NOT_READY, "PSP route snapshot is incomplete");
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
        route.setPspCallbackUrl(callbackUrlBuilder.payinCallbackUrl(order.getPspAccountNo()));
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

    private void applyDispatchResult(PayinOrderEntity order, PspPayDispatchResult result) {
        if (result == null) {
            throw new ApiException(ApiErrorCode.SYSTEM_ERROR, "PSP payin submit result is empty");
        }
        String fromStatus = order.getStatus();
        String toStatus = result.isSuccess() ? PayinOrderStatusEnum.PROCESSING.code() : PayinOrderStatusEnum.FAILED.code();

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

        if (result.isSuccess()) {
            wrapper.set("merchant_status_code", MerchantOrderStatusEnum.PROCESSING.code())
                    .set("merchant_status_reason", MerchantOrderStatusEnum.PROCESSING.statusReason())
                    .set("submitted_at", Instant.now())
                    .set("next_query_at", Instant.now().plusSeconds(firstQueryDelaySeconds()));
        } else {
            String reason = StringUtils.defaultIfBlank(
                    result.getErrorMessage(),
                    StringUtils.defaultIfBlank(result.getResponseMessage(), "PSP submit failed")
            );
            wrapper.set("status_reason", StringUtils.left(reason, 512))
                    .set("merchant_status_code", MerchantOrderStatusEnum.FAILED.code())
                    .set("merchant_status_reason", MerchantOrderStatusEnum.FAILED.statusReason())
                    .set("failed_at", Instant.now())
                    .set("next_query_at", null);
        }

        if (payinOrderDao.update(null, wrapper) > 0) {
            recordStatusChange(order, fromStatus, toStatus,
                    result.isSuccess() ? "PSP_SUBMIT" : "PSP_SUBMIT_FAILED",
                    result.isSuccess() ? null : StringUtils.defaultIfBlank(result.getErrorMessage(), result.getResponseMessage()));
        }
    }

    private void recordStatusChange(PayinOrderEntity order,
                                    String fromStatus,
                                    String toStatus,
                                    String eventType,
                                    String reason) {
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
                "SYSTEM",
                order.getAppId(),
                order.getMerchantOrderNo(),
                null
        ));
    }

    private long firstQueryDelaySeconds() {
        List<Long> backoffSeconds = configService.pspQueryConfig().getBackoffSeconds();
        if (backoffSeconds == null || backoffSeconds.isEmpty()) {
            return 60L;
        }
        return Math.max(1L, backoffSeconds.getFirst());
    }

    private String pspBankCode(String routeSnapshotJson) {
        if (StringUtils.isBlank(routeSnapshotJson)) {
            return null;
        }
        try {
            JSONObject snapshot = JSON.parseObject(routeSnapshotJson);
            return snapshot == null ? null : StringUtils.trimToNull(snapshot.getString("pspBankCode"));
        } catch (Exception ex) {
            return null;
        }
    }
}
