package com.gk.payment.callback;

import com.alibaba.fastjson2.JSON;
import com.gk.common.transaction.SavepointExecutor;
import com.gk.payment.domain.enums.BizTypeEnum;
import com.gk.payment.domain.enums.PayDirectionEnum;
import com.gk.payment.domain.enums.SignTypeEnum;
import com.gk.payment.domain.key.BizKeyUtils;
import com.gk.payment.config.MerchantNotifyConfig;
import com.gk.payment.config.PaymentConfigService;
import com.gk.payment.dao.MerchantNotifyTaskDao;
import com.gk.payment.entity.MerchantNotifyTaskEntity;
import com.gk.payment.merchantview.MerchantOrderView;
import com.gk.payment.merchantview.MerchantOrderViewAssembler;
import com.gk.payment.merchantview.MerchantNotifyOrderView;
import com.gk.payment.merchantview.PayinOrderView;
import com.gk.payment.merchantview.PayoutOrderView;
import com.gk.payment.notify.MerchantOrderNotifyStatusService;
import com.gk.psp.callback.model.PspCallbackOrder;
import com.gk.psp.callback.model.PspCallbackResult;
import com.gk.psp.callback.support.PspCallbackUtils;
import com.gk.psp.entity.PspCallbackLogEntity;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Creates merchant notify tasks after a PSP callback moves an order forward.
 */
@Component
@RequiredArgsConstructor
public class PspCallbackNotifyCreator {
    private final MerchantNotifyTaskDao merchantNotifyTaskDao;
    private final MerchantOrderNotifyStatusService merchantOrderNotifyStatusService;
    private final PaymentConfigService configService;
    private final MerchantOrderViewAssembler merchantOrderViewAssembler;

    public void create(String bizType, PspCallbackResult result, PspCallbackOrder order, PspCallbackLogEntity logEntity) {
        if (StringUtils.isBlank(order.notifyUrl())) {
            return;
        }
        MerchantNotifyOrderView payload = payload(bizType, result, order);
        String payloadJson = JSON.toJSONString(payload);
        MerchantNotifyTaskEntity task = new MerchantNotifyTaskEntity();
        task.setTenantId(order.tenantId());
        task.setMerchantId(order.merchantId());
        task.setMerchantAppId(order.merchantAppId());
        task.setAppId(order.appId());
        task.setTaskNo(BizKeyUtils.genMerchantNotifyTaskNo());
        task.setBizType(bizType);
        task.setBizId(order.id());
        task.setBizNo(order.orderNo());
        task.setEventType(eventType(bizType, result.getOrderStatus()));
        task.setSourceEventId(logEntity == null || logEntity.getId() == null ? null : String.valueOf(logEntity.getId()));
        task.setNotifyUrl(order.notifyUrl());
        task.setHttpMethod("POST");
        task.setContentType("application/json");
        task.setCharset("UTF-8");
        task.setSignType(SignTypeEnum.HMAC_SHA256.code());
        task.setPayloadHash(PspCallbackUtils.sha256Hex(payloadJson));
        task.setPayloadJson(payloadJson);
        MerchantNotifyConfig config = configService.merchantNotify();
        task.setTimeoutMs(config.getReadTimeoutMs());
        task.setStatus("INIT");
        task.setRetryCount(0);
        task.setMaxRetryCount(config.getMaxRetryCount());
        task.setNextRetryAt(Instant.now());
        task.setTraceId(logEntity == null ? null : logEntity.getTraceId());
        try {
            SavepointExecutor.run(() -> merchantNotifyTaskDao.insert(task));
            merchantOrderNotifyStatusService.onTaskCreated(bizType, order.id(), task.getId());
        } catch (DuplicateKeyException ignored) {
            // Duplicate terminal callbacks can race to create the same notify task.
        }
    }

    MerchantNotifyOrderView payload(String bizType, PspCallbackResult result, PspCallbackOrder order) {
        boolean payinOrder = BizTypeEnum.PAYIN_ORDER.matches(bizType);
        String direction = payinOrder ? PayDirectionEnum.PAYIN.code() : PayDirectionEnum.PAYOUT.code();
        MerchantOrderView view = payinOrder
                ? merchantOrderViewAssembler.fromPayinCallback(order, result)
                : merchantOrderViewAssembler.fromPayoutCallback(order, result);

        MerchantNotifyOrderView payload = new MerchantNotifyOrderView();
        payload.setMerchantId(StringUtils.trimToNull(order.merchantNo()));
        payload.setAppId(StringUtils.trimToNull(order.appId()));
        payload.setDirection(direction);
        copyOrderView(payload, view);
        return payload;
    }

    private void copyOrderView(MerchantNotifyOrderView payload, MerchantOrderView view) {
        payload.setSystemOrderId(StringUtils.trimToNull(view.getSystemOrderId()));
        payload.setMerchantOrderId(StringUtils.trimToNull(view.getMerchantOrderId()));
        payload.setStatus(StringUtils.trimToNull(view.getStatus()));
        payload.setStatusReason(StringUtils.trimToNull(view.getStatusReason()));
        payload.setAmount(StringUtils.trimToNull(view.getAmount()));
        payload.setCurrency(StringUtils.trimToNull(view.getCurrency()));
        payload.setCountryCode(StringUtils.trimToNull(view.getCountryCode()));
        payload.setMethodCode(StringUtils.trimToNull(view.getMethodCode()));
        payload.setFeeAmount(StringUtils.trimToNull(view.getFeeAmount()));
        if (view instanceof PayinOrderView payinView) {
            payload.setPayUrl(StringUtils.trimToNull(payinView.getPayUrl()));
            payload.setPaidAmount(StringUtils.trimToNull(payinView.getPaidAmount()));
            payload.setSettleAmount(StringUtils.trimToNull(payinView.getSettleAmount()));
        }
        if (view instanceof PayoutOrderView payoutView) {
            payload.setDebitAmount(StringUtils.trimToNull(payoutView.getDebitAmount()));
        }
    }

    private String eventType(String bizType, String status) {
        String prefix = BizTypeEnum.PAYIN_ORDER.matches(bizType) ? PayDirectionEnum.PAYIN.code() : PayDirectionEnum.PAYOUT.code();
        return prefix + "_" + PspCallbackUtils.normalizeStatus(status);
    }

}
