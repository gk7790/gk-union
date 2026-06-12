package com.gk.psp.callback.support;

import com.alibaba.fastjson2.JSON;
import com.gk.common.enums.BizTypeEnum;
import com.gk.common.utils.BizKeyUtils;
import com.gk.payment.dao.MerchantNotifyTaskDao;
import com.gk.payment.entity.MerchantNotifyTaskEntity;
import com.gk.payment.notify.MerchantOrderNotifyStatusService;
import com.gk.psp.callback.model.PspCallbackOrder;
import com.gk.psp.callback.model.PspCallbackResult;
import com.gk.psp.entity.PspCallbackLogEntity;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * PSP 终态回调后创建商户异步通知任务。
 * <p>
 * 通知报文字段与商户 Open API 统一: snake_case, system_order_id / merchant_order_id 为业务单号(非数据库主键)。
 */
@Component
@RequiredArgsConstructor
public class PspCallbackNotifyCreator {
    private final MerchantNotifyTaskDao merchantNotifyTaskDao;
    private final MerchantOrderNotifyStatusService merchantOrderNotifyStatusService;

    public void create(String bizType, PspCallbackResult result, PspCallbackOrder order, PspCallbackLogEntity logEntity) {
        if (StringUtils.isBlank(order.notifyUrl())) {
            return;
        }
        String payloadJson = JSON.toJSONString(payload(bizType, result, order));
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
        task.setSignType("MD5");
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
            merchantOrderNotifyStatusService.onTaskCreated(bizType, order.id(), task.getId());
        } catch (DuplicateKeyException ignored) {
            // Duplicate terminal callbacks may attempt to create the same notification task.
        }
    }

    private Map<String, Object> payload(String bizType, PspCallbackResult result, PspCallbackOrder order) {
        boolean payOrder = BizTypeEnum.PAY_ORDER.matches(bizType);
        String orderType = payOrder ? "PAY" : "PAYOUT";
        String orderStatus = eventType(bizType, result.getOrderStatus());

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("merchant_id", order.merchantNo());
        payload.put("app_id", order.appId());
        payload.put("order_type", orderType);
        payload.put("system_order_id", order.orderNo());
        payload.put("merchant_order_id", order.merchantOrderNo());
        payload.put("currency", order.currency());
        payload.put("amount", decimal(order.amount()));
        payload.put("order_status", orderStatus);
        payload.put("msg", message(orderStatus, result));

        if (payOrder) {
            BigDecimal paidAmount = PspCallbackUtils.defaultAmount(result.getAmount(), order.amount());
            payload.put("paid_amount", decimal(paidAmount));
            if (positive(order.settleAmount())) {
                payload.put("settle_amount", decimal(order.settleAmount()));
            }
        } else {
            BigDecimal debitAmount = order.totalDebitAmount() != null && order.totalDebitAmount().signum() > 0
                    ? order.totalDebitAmount()
                    : order.amount();
            payload.put("debit_amount", decimal(debitAmount));
        }
        if (positive(order.merchantFeeAmount())) {
            payload.put("fee_amount", decimal(order.merchantFeeAmount()));
        }
        return payload;
    }

    private String message(String orderStatus, PspCallbackResult result) {
        if (orderStatus.endsWith("_SUCCESS")) {
            return "Transaction success";
        }
        return StringUtils.defaultIfBlank(result.getErrorMessage(), "Transaction failed");
    }

    private String eventType(String bizType, String status) {
        String prefix = BizTypeEnum.PAY_ORDER.matches(bizType) ? "PAY" : "PAYOUT";
        return prefix + "_" + PspCallbackUtils.normalizeStatus(status);
    }

    private boolean positive(BigDecimal value) {
        return value != null && value.signum() > 0;
    }

    private String decimal(BigDecimal value) {
        return PspCallbackUtils.decimalText(value);
    }
}
