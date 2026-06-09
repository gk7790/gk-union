package com.gk.psp.callback.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gk.common.utils.BizKeyUtils;
import com.gk.payment.dao.MerchantNotifyTaskDao;
import com.gk.payment.entity.MerchantNotifyTaskEntity;
import com.gk.psp.callback.model.PspCallbackOrder;
import com.gk.psp.callback.model.PspCallbackResult;
import com.gk.psp.entity.PspCallbackLogEntity;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class PspCallbackNotifyCreator {
    private final MerchantNotifyTaskDao merchantNotifyTaskDao;
    private final ObjectMapper objectMapper;

    public void create(String bizType, PspCallbackResult result, PspCallbackOrder order, PspCallbackLogEntity logEntity) {
        if (StringUtils.isBlank(order.notifyUrl())) {
            return;
        }
        String payloadJson = toJson(payload(bizType, result, order));
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
        task.setSignType("HMAC_SHA256");
        task.setPayloadHash(PspCallbackUtils.sha256Hex(payloadJson));
        task.setPayloadJson(payloadJson);
        task.setTimeoutMs(5000);
        task.setStatus("INIT");
        task.setRetryCount(0);
        task.setMaxRetryCount(16);
        task.setNextRetryAt(Instant.now());
        task.setTraceId(logEntity == null ? null : logEntity.getTraceId());
        try {
            merchantNotifyTaskDao.insert(task);
        } catch (DuplicateKeyException ignored) {
            // Duplicate terminal callbacks may attempt to create the same notification task.
        }
    }

    private Map<String, Object> payload(String bizType, PspCallbackResult result, PspCallbackOrder order) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("merchant_order_id", order.merchantOrderNo());
        payload.put("system_order_id", order.orderNo());
        payload.put("order_type", bizType);
        payload.put("status", PspCallbackUtils.normalizeStatus(result.getOrderStatus()));
        payload.put("amount", PspCallbackUtils.decimalText(PspCallbackUtils.defaultAmount(result.getAmount(), order.amount())));
        payload.put("currency", StringUtils.defaultIfBlank(result.getCurrency(), order.currency()));
        payload.put("psp_order_no", StringUtils.defaultIfBlank(result.getPspOrderNo(), order.pspOrderNo()));
        return payload;
    }

    private String eventType(String bizType, String status) {
        String prefix = PspCallbackConstants.BIZ_TYPE_PAY_ORDER.equals(bizType) ? "PAY" : "PAYOUT";
        return prefix + "_" + PspCallbackUtils.normalizeStatus(status);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new IllegalStateException("Build merchant notify payload failed", ex);
        }
    }
}
