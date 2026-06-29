package com.gk.payment.callback;

import com.alibaba.fastjson2.JSON;
import com.gk.common.enums.BizTypeEnum;
import com.gk.common.utils.BizKeyUtils;
import com.gk.payment.dao.MerchantNotifyTaskDao;
import com.gk.payment.entity.MerchantNotifyTaskEntity;
import com.gk.payment.notify.MerchantOrderNotifyStatusService;
import com.gk.openapi.util.ApiAmountUtils;
import com.gk.psp.callback.model.PspCallbackOrder;
import com.gk.psp.callback.model.PspCallbackResult;
import com.gk.psp.callback.support.PspCallbackUtils;
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
 * PSP 终态回调后的商户通知任务创建器 * <p>
 * PSP 回调把平台订单推进到终态后，本类负责创建商户异步通知任务 * 通知报文字段与商OpenAPI 保持一致，使用 snake_case * system_order_id merchant_order_id 表示业务单号，不是数据库主键 */
@Component
@RequiredArgsConstructor
public class PspCallbackNotifyCreator {
    private final MerchantNotifyTaskDao merchantNotifyTaskDao;
    private final MerchantOrderNotifyStatusService merchantOrderNotifyStatusService;

    /**
     * 创建商户异步通知任务     *
     * @param bizType 业务类型，代收或代付
     * @param result PSP 标准回调结果
     * @param order 平台订单快照
     * @param logEntity PSP 回调日志，用于关联来源事件和 traceId
     */
    public void create(String bizType, PspCallbackResult result, PspCallbackOrder order, PspCallbackLogEntity logEntity) {
        if (StringUtils.isBlank(order.notifyUrl())) {
            // 商户未配notifyUrl 时，不创建通知任务
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
            // 通知任务入库后，同步更新订单通知状态，便于商户侧查询通知进度
                        merchantNotifyTaskDao.insert(task);
            merchantOrderNotifyStatusService.onTaskCreated(bizType, order.id(), task.getId());
        } catch (DuplicateKeyException ignored) {
            // 重复终态回调可能尝试创建同一笔通知任务，唯一键冲突时直接忽略
            }
    }

    /**
     * 构建商户通知报文     * <p>
     * 代收包含 paid_amount、settle_amount；代付包debit_amount     * 有手续费时统一输出 fee_amount     */
    Map<String, Object> payload(String bizType, PspCallbackResult result, PspCallbackOrder order) {
        boolean payOrder = BizTypeEnum.PAY_ORDER.matches(bizType);
        String direction = payOrder ? "PAYIN" : "PAYOUT";
        String orderStatus = eventType(bizType, result.getOrderStatus());

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("merchant_id", order.merchantNo());
        payload.put("app_id", order.appId());
        payload.put("direction", direction);
        payload.put("system_order_id", order.orderNo());
        payload.put("merchant_order_id", order.merchantOrderNo());
        payload.put("currency", order.currency());
        payload.put("amount", money(order.amount(), order.currency()));
        payload.put("order_status", orderStatus);
        if (!orderStatus.endsWith("_SUCCESS")) {
            payload.put("reason", reason(result));
        }

        if (payOrder) {
            // PSP 未回传实际支付金额时，默认使用订单金额
                        BigDecimal paidAmount = PspCallbackUtils.defaultAmount(result.getAmount(), order.amount());
            payload.put("paid_amount", money(paidAmount, order.currency()));
            if (positive(order.settleAmount())) {
                payload.put("settle_amount", money(order.settleAmount(), order.currency()));
            }
        } else {
            // 代付优先使用包含手续费的总扣款金额，没有时回退到订单金额
                        BigDecimal debitAmount = order.totalDebitAmount() != null && order.totalDebitAmount().signum() > 0
                    ? order.totalDebitAmount()
                    : order.amount();
            payload.put("debit_amount", money(debitAmount, order.currency()));
        }
        if (positive(order.merchantFeeAmount())) {
            payload.put("fee_amount", money(order.merchantFeeAmount(), order.currency()));
        }
        return payload;
    }

    /**
     * 生成商户通知失败原因     */
    private String reason(PspCallbackResult result) {
        return StringUtils.defaultIfBlank(result.getErrorMessage(), "Transaction failed");
    }

    /**
     * 生成商户通知事件类型     */
    private String eventType(String bizType, String status) {
        String prefix = BizTypeEnum.PAY_ORDER.matches(bizType) ? "PAY" : "PAYOUT";
        return prefix + "_" + PspCallbackUtils.normalizeStatus(status);
    }

    /**
     * 判断金额是否大于 0     */
    private boolean positive(BigDecimal value) {
        return value != null && value.signum() > 0;
    }

    /**
     * 转换金额为普通文本     */
    private String money(BigDecimal value, String currency) {
        return ApiAmountUtils.formatCurrencyAmount(value, currency);
    }
}
